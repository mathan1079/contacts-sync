export interface Env {
	DB: D1Database;

	BACKUPS: R2Bucket;

	SYNC_API_TOKEN: string;

	ENVIRONMENT: string;
}

// ======================================================
// TYPES
// ======================================================

interface ContactPhone {
	number: string;
	normalizedNumber?: string | null;
	type?: string | null;
}

interface ContactEmail {
	email: string;
	type?: string | null;
}

interface ContactDto {
	contactId: number;

	lookupKey?: string | null;

	displayName?: string | null;

	phones: ContactPhone[];

	emails: ContactEmail[];

	company?: string | null;

	designation?: string | null;

	lastUpdatedTimestamp?: number | null;
}

interface ContactSyncRequest {
	deviceId: string;

	syncTimestamp: number;

	upserts: ContactDto[];

	deletedContactIds: number[];
}

interface ApiError {
	success: false;
	message: string;
}

interface ApiSuccess {
	success: true;

	message: string;

	processed: {
		upserted: number;
		deleted: number;
	};

	serverTimestamp: number;
}

// ======================================================
// WORKER
// ======================================================

export default {
	async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
		try {
			const url = new URL(request.url);

			// -----------------------------------------------
			// CORS pre-flight
			// -----------------------------------------------

			if (request.method === 'OPTIONS') {
				return new Response(null, {
					status: 204,
					headers: corsHeaders(),
				});
			}

			// -----------------------------------------------
			// HEALTH
			// -----------------------------------------------

			if (request.method === 'GET' && url.pathname === '/health') {
				return json({
					success: true,

					service: 'contact-sync-api',

					environment: env.ENVIRONMENT,

					timestamp: Date.now(),
				});
			}

			// -----------------------------------------------
			// CONTACT SYNC
			// -----------------------------------------------

			if (request.method === 'POST' && url.pathname === '/api/v1/contact-sync') {
				return await handleContactSync(request, env, ctx);
			}

			// -----------------------------------------------
			// GET CONTACTS
			// -----------------------------------------------

			if (request.method === 'GET' && url.pathname === '/api/v1/contacts') {
				const authFailure = authorize(request, env);

				if (authFailure) {
					return authFailure;
				}

				return await getContacts(url, env);
			}
			return json(
				{
					success: false,
					message: 'Route not found',
				},
				404,
			);
		} catch (error) {
			console.error('Unhandled worker error', error);

			return json(
				{
					success: false,

					message: 'Internal server error',
				},
				500,
			);
		}
	},
};

// ======================================================
// CONTACT SYNC
// ======================================================

async function handleContactSync(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
	// ----------------------------------------------------
	// AUTHENTICATION
	// ----------------------------------------------------

	const authFailure = authorize(request, env);

	if (authFailure) {
		return authFailure;
	}

	// ----------------------------------------------------
	// CONTENT TYPE
	// ----------------------------------------------------

	const contentType = request.headers.get('content-type') || '';

	if (!contentType.toLowerCase().includes('application/json')) {
		return json(
			{
				success: false,

				message: 'Content-Type must be application/json',
			},
			415,
		);
	}

	// ----------------------------------------------------
	// PARSE BODY
	// ----------------------------------------------------

	let body: unknown;

	try {
		body = await request.json();
	} catch {
		return json(
			{
				success: false,

				message: 'Invalid JSON body',
			},
			400,
		);
	}

	// ----------------------------------------------------
	// VALIDATION
	// ----------------------------------------------------

	const validation = validateSyncRequest(body);

	if (!validation.valid) {
		return json(
			{
				success: false,

				message: validation.message,
			},
			400,
		);
	}

	const payload = body as ContactSyncRequest;

	// ----------------------------------------------------
	// SANITY LIMITS
	// ----------------------------------------------------

	if (payload.upserts.length > 10_000) {
		return json(
			{
				success: false,

				message: 'Too many upserts in one request',
			},
			413,
		);
	}

	if (payload.deletedContactIds.length > 10_000) {
		return json(
			{
				success: false,

				message: 'Too many deletes in one request',
			},
			413,
		);
	}

	// ----------------------------------------------------
	// PREVENT SAME CONTACT BEING UPDATED + DELETED
	// ----------------------------------------------------

	const deleteSet = new Set(payload.deletedContactIds);

	for (const contact of payload.upserts) {
		if (deleteSet.has(contact.contactId)) {
			return json(
				{
					success: false,

					message: `Contact ${contact.contactId} exists in both upserts and deletes`,
				},
				400,
			);
		}
	}

	const now = Date.now();

	const syncId = crypto.randomUUID();

	try {
		// ==================================================
		// DEVICE
		// ==================================================

		await env.DB.prepare(
			`
        INSERT INTO devices (
          device_id,
          first_seen_at,
          last_seen_at,
          last_client_sync_timestamp,
          total_syncs
        )
        VALUES (?, ?, ?, ?, 1)

        ON CONFLICT(device_id)
        DO UPDATE SET

          last_seen_at =
            excluded.last_seen_at,

          last_client_sync_timestamp =
            excluded.last_client_sync_timestamp,

          total_syncs =
            devices.total_syncs + 1
        `,
		)
			.bind(payload.deviceId, now, now, payload.syncTimestamp)
			.run();

		// ==================================================
		// CONTACT UPSERT STATEMENTS
		// ==================================================

		const statements: D1PreparedStatement[] = [];

		for (const contact of payload.upserts) {
			statements.push(
				env.DB.prepare(
					`
            INSERT INTO contacts (

              device_id,

              contact_id,

              lookup_key,

              display_name,

              phones_json,

              emails_json,

              company,

              designation,

              contact_last_updated_timestamp,

              server_created_at,

              server_updated_at
            )

            VALUES (
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )

            ON CONFLICT(
              device_id,
              contact_id
            )

            DO UPDATE SET

              lookup_key =
                excluded.lookup_key,

              display_name =
                excluded.display_name,

              phones_json =
                excluded.phones_json,

              emails_json =
                excluded.emails_json,

              company =
                excluded.company,

              designation =
                excluded.designation,

              contact_last_updated_timestamp =
                excluded.contact_last_updated_timestamp,

              server_updated_at =
                excluded.server_updated_at
            `,
				)

					.bind(
						payload.deviceId,

						contact.contactId,

						contact.lookupKey ?? null,

						contact.displayName ?? null,

						JSON.stringify(contact.phones || []),

						JSON.stringify(contact.emails || []),

						contact.company ?? null,

						contact.designation ?? null,

						contact.lastUpdatedTimestamp ?? null,

						now,

						now,
					),
			);
		}

		// ==================================================
		// DELETE STATEMENTS
		// ==================================================

		for (const contactId of payload.deletedContactIds) {
			statements.push(
				env.DB.prepare(
					`
            DELETE FROM contacts

            WHERE device_id = ?

            AND contact_id = ?
            `,
				)

					.bind(payload.deviceId, contactId),
			);
		}

		// ==================================================
		// EXECUTE CHUNKS
		// ==================================================

		/*
		 * Do not create one enormous database batch.
		 *
		 * Replaying a chunk is safe because:
		 *
		 * INSERT = UPSERT
		 * DELETE = idempotent DELETE
		 */

		const chunkSize = 100;

		for (let i = 0; i < statements.length; i += chunkSize) {
			const chunk = statements.slice(i, i + chunkSize);

			if (chunk.length > 0) {
				await env.DB.batch(chunk);
			}
		}

		// ==================================================
		// SYNC LOG
		// ==================================================

		await env.DB.prepare(
			`
        INSERT INTO sync_logs (

          id,

          device_id,

          client_sync_timestamp,

          server_sync_timestamp,

          upsert_count,

          delete_count,

          status
        )

        VALUES (
          ?, ?, ?, ?, ?, ?, ?
        )
        `,
		)

			.bind(
				syncId,

				payload.deviceId,

				payload.syncTimestamp,

				now,

				payload.upserts.length,

				payload.deletedContactIds.length,

				'SUCCESS',
			)

			.run();

		// ==================================================
		// R2 AUDIT BACKUP
		// ==================================================

		/*
		 * R2 is being used for object/archive storage,
		 * not relational contact storage.
		 *
		 * We do not block the API response waiting
		 * for this backup.
		 */

		ctx.waitUntil(saveSyncBackup(payload, syncId, now, env));

		// ==================================================
		// RESPONSE
		// ==================================================

		const response: ApiSuccess = {
			success: true,

			message: 'Contacts synchronized successfully',

			processed: {
				upserted: payload.upserts.length,

				deleted: payload.deletedContactIds.length,
			},

			serverTimestamp: now,
		};

		return json(response, 200);
	} catch (error) {
		console.error('Contact sync failed', error);

		// -----------------------------------------------
		// Failure log
		// -----------------------------------------------

		try {
			await env.DB.prepare(
				`
          INSERT INTO sync_logs (

            id,

            device_id,

            client_sync_timestamp,

            server_sync_timestamp,

            upsert_count,

            delete_count,

            status
          )

          VALUES (
            ?, ?, ?, ?, ?, ?, ?
          )
          `,
			)

				.bind(
					syncId,

					payload.deviceId,

					payload.syncTimestamp,

					now,

					payload.upserts.length,

					payload.deletedContactIds.length,

					'FAILED',
				)

				.run();
		} catch (logError) {
			console.error('Unable to create failure log', logError);
		}

		return json(
			{
				success: false,

				message: 'Unable to synchronize contacts',
			},
			500,
		);
	}
}

// ======================================================
// R2 BACKUP
// ======================================================

async function saveSyncBackup(payload: ContactSyncRequest, syncId: string, timestamp: number, env: Env): Promise<void> {
	try {
		const date = new Date(timestamp);

		const yyyy = date.getUTCFullYear().toString();

		const month = String(date.getUTCMonth() + 1).padStart(2, '0');

		const day = String(date.getUTCDate()).padStart(2, '0');

		const safeDeviceId = encodeURIComponent(payload.deviceId);

		const key = `sync-events/${safeDeviceId}/${yyyy}/${month}/${day}/${timestamp}-${syncId}.json`;

		await env.BACKUPS.put(
			key,

			JSON.stringify({
				syncId,

				receivedAt: timestamp,

				payload,
			}),

			{
				httpMetadata: {
					contentType: 'application/json',
				},

				customMetadata: {
					deviceId: payload.deviceId,

					syncId,
				},
			},
		);
	} catch (error) {
		/*
		 * Backup failure should NOT cause Android
		 * to repeat an already committed D1 sync.
		 */

		console.error('R2 backup failed', error);
	}
}

// ======================================================
// GET CONTACTS
// ======================================================

async function getContacts(url: URL, env: Env): Promise<Response> {
	const requestedLimit = Number(url.searchParams.get('limit') || '500');

	const requestedOffset = Number(url.searchParams.get('offset') || '0');

	const limit = Math.min(Math.max(requestedLimit, 1), 500);

	const offset = Math.max(requestedOffset, 0);

	const result = await env.DB.prepare(
		`
        SELECT

          contact_id,

          lookup_key,

          display_name,

          phones_json,

          emails_json,

          company,

          designation,

          contact_last_updated_timestamp,

          server_created_at,

          server_updated_at

        FROM contacts

        ORDER BY display_name COLLATE NOCASE

        LIMIT ?

        OFFSET ?
        `,
	)

		.bind(limit, offset)

		.all();

	const contacts = result.results.map((row: any) => ({
		contactId: row.contact_id,

		lookupKey: row.lookup_key,

		displayName: row.display_name,

		phones: safeParseJson(row.phones_json, []),

		emails: safeParseJson(row.emails_json, []),

		company: row.company,

		designation: row.designation,

		lastUpdatedTimestamp: row.contact_last_updated_timestamp,

		serverCreatedAt: row.server_created_at,

		serverUpdatedAt: row.server_updated_at,
	}));

	return json({
		success: true,

		limit,

		offset,

		count: contacts.length,

		contacts,
	});
}

// ======================================================
// AUTH
// ======================================================

function authorize(request: Request, env: Env): Response | null {
	const authorization = request.headers.get('authorization');

	if (!authorization) {
		return json(
			{
				success: false,

				message: 'Authorization header missing',
			},
			401,
		);
	}

	const prefix = 'Bearer ';

	if (!authorization.startsWith(prefix)) {
		return json(
			{
				success: false,

				message: 'Invalid authorization format',
			},
			401,
		);
	}

	const token = authorization.substring(prefix.length).trim();

	if (!token || token !== env.SYNC_API_TOKEN) {
		return json(
			{
				success: false,

				message: 'Invalid API token',
			},
			401,
		);
	}

	return null;
}

// ======================================================
// VALIDATION
// ======================================================

function validateSyncRequest(value: unknown):
	| {
			valid: true;
	  }
	| {
			valid: false;
			message: string;
	  } {
	if (!value || typeof value !== 'object') {
		return {
			valid: false,
			message: 'Body must be an object',
		};
	}

	const body = value as Record<string, unknown>;

	if (typeof body.deviceId !== 'string' || !isValidDeviceId(body.deviceId)) {
		return {
			valid: false,
			message: 'Invalid deviceId',
		};
	}

	if (typeof body.syncTimestamp !== 'number' || !Number.isFinite(body.syncTimestamp)) {
		return {
			valid: false,
			message: 'Invalid syncTimestamp',
		};
	}

	if (!Array.isArray(body.upserts)) {
		return {
			valid: false,
			message: 'upserts must be an array',
		};
	}

	if (!Array.isArray(body.deletedContactIds)) {
		return {
			valid: false,

			message: 'deletedContactIds must be an array',
		};
	}

	for (const deletedId of body.deletedContactIds) {
		if (typeof deletedId !== 'number' || !Number.isSafeInteger(deletedId) || deletedId < 0) {
			return {
				valid: false,

				message: 'deletedContactIds contains an invalid contact ID',
			};
		}
	}

	const seenContactIds = new Set<number>();

	for (const item of body.upserts) {
		if (!item || typeof item !== 'object') {
			return {
				valid: false,

				message: 'Invalid contact object',
			};
		}

		const contact = item as Record<string, unknown>;

		if (typeof contact.contactId !== 'number' || !Number.isSafeInteger(contact.contactId) || contact.contactId < 0) {
			return {
				valid: false,

				message: 'Invalid contactId',
			};
		}

		if (seenContactIds.has(contact.contactId)) {
			return {
				valid: false,

				message: `Duplicate contactId ${contact.contactId}`,
			};
		}

		seenContactIds.add(contact.contactId);

		if (contact.phones !== undefined && !Array.isArray(contact.phones)) {
			return {
				valid: false,

				message: 'phones must be an array',
			};
		}

		if (contact.emails !== undefined && !Array.isArray(contact.emails)) {
			return {
				valid: false,

				message: 'emails must be an array',
			};
		}
	}

	return {
		valid: true,
	};
}

function isValidDeviceId(value: string): boolean {
	return value.length >= 8 && value.length <= 200 && /^[a-zA-Z0-9._:-]+$/.test(value);
}

// ======================================================
// HELPERS
// ======================================================

function safeParseJson<T>(value: unknown, fallback: T): T {
	if (typeof value !== 'string') {
		return fallback;
	}

	try {
		return JSON.parse(value) as T;
	} catch {
		return fallback;
	}
}

function json(data: unknown, status = 200): Response {
	return new Response(JSON.stringify(data), {
		status,

		headers: {
			'Content-Type': 'application/json; charset=utf-8',

			...corsHeaders(),
		},
	});
}

function corsHeaders(): Record<string, string> {
	return {
		/*
		 * Android applications do not require CORS,
		 * but keeping this permits a future web admin UI.
		 */

		'Access-Control-Allow-Origin': '*',

		'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',

		'Access-Control-Allow-Headers': 'Authorization, Content-Type',

		'Cache-Control': 'no-store',
	};
}

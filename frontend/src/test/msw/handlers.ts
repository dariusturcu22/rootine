import type { RequestHandler } from 'msw'

/** Default handlers shared by all tests. Add per-test handlers with server.use(...). */
export const handlers: RequestHandler[] = []

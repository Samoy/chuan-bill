export const EVENTS = {
  BILL: {
    UPDATED: 'bill:updated',
    CREATED: 'bill:created',
    DELETED: 'bill:deleted',
  },
  FAMILY: {
    UPDATED: 'family:updated',
    MEMBER_CHANGED: 'family:member:changed',
  },
  STATISTICS: {
    UPDATED: 'statistics:updated',
  },
  USER: {
    UPDATED: 'user:updated',
    LOGOUT: 'user:logout',
  },
} as const

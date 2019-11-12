export const authState = {
  accessToken: '',
  refreshToken: '',
  isAuthenticated: false,
}

export function authReducer(state = authState, action) {
  switch (action.type) {
    case 'AUTH_USER':
      return { ...state, isAuthenticated: !state.isAuthenticated }
    default:
      return state
  }
}
export const ACTIONS = {
  INCREMENT: 'INCREMENT',
  DECREMENT: 'DECREMENT',
}

export const INITIAL_STATE = {
  thumbnailCount: 8,
  isMin: true,
  isMax: false,
}

export const reducer = (state, action) => {
  switch (action.type) {
    case 'DECREMENT':
      if (state.thumbnailCount === 1)
        return { ...state, thumbnailCount: 4, isMax: false }
      if (state.thumbnailCount === 4)
        return { ...state, thumbnailCount: 8, isMin: true }
      return state
    case 'INCREMENT':
      if (state.thumbnailCount === 8)
        return { ...state, thumbnailCount: 4, isMin: false }
      if (state.thumbnailCount === 4)
        return { ...state, thumbnailCount: 1, isMax: true }
      return state
    default:
      return state
  }
}

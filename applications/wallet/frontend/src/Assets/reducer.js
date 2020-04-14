export const ACTIONS = {
  INCREMENT: 'INCREMENT',
  DECREMENT: 'DECREMENT',
}

export const INITIAL_STATE = {
  columnCount: 8,
  isMin: true,
  isMax: false,
}

export const reducer = (state, action) => {
  switch (action.type) {
    case 'DECREMENT':
      if (state.columnCount === 1)
        return { ...state, columnCount: 4, isMax: false }
      if (state.columnCount === 4)
        return { ...state, columnCount: 8, isMin: true }
      return state
    case 'INCREMENT':
      if (state.columnCount === 8)
        return { ...state, columnCount: 4, isMin: false }
      if (state.columnCount === 4)
        return { ...state, columnCount: 1, isMax: true }
      return state
    default:
      return state
  }
}

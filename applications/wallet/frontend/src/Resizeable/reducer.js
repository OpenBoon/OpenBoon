export const ACTIONS = {
  TOGGLE_OPEN: 'TOGGLE_OPEN',
  HANDLE_MOUSE_MOVE: 'HANDLE_MOUSE_MOVE',
  HANDLE_MOUSE_UP: 'HANDLE_MOUSE_UP',
  OPEN: 'OPEN',
  CLOSE: 'CLOSE',
  UPDATE: 'UPDATE',
}

export const reducer = (
  state,
  { type: actionType, payload: { minSize, newSize, ...rest } = {} },
) => {
  switch (actionType) {
    case ACTIONS.TOGGLE_OPEN: {
      return {
        ...state,
        originSize: state.isOpen ? 0 : minSize,
        isOpen: !state.isOpen,
      }
    }

    case ACTIONS.HANDLE_MOUSE_MOVE: {
      return {
        ...state,
        size: newSize,
        isOpen: newSize > 0,
      }
    }

    case ACTIONS.HANDLE_MOUSE_UP: {
      if (newSize > minSize) {
        return {
          ...state,
          ...rest,
          size: newSize,
          isOpen: true,
        }
      }

      return {
        ...state,
        ...rest,
        size: minSize,
        originSize: state.isOpen ? 0 : minSize,
        isOpen: !state.isOpen,
      }
    }

    case ACTIONS.OPEN: {
      return {
        ...state,
        ...rest,
        originSize: minSize,
        isOpen: true,
      }
    }

    case ACTIONS.CLOSE: {
      return {
        ...state,
        ...rest,
        originSize: 0,
        isOpen: false,
      }
    }

    case ACTIONS.UPDATE: {
      return {
        ...state,
        ...rest,
      }
    }

    default:
      return state
  }
}

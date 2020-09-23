export const ACTIONS = {
  TOGGLE_OPEN: 'TOGGLE_OPEN',
  TOGGLE_VISIBLE: 'TOGGLE_VISIBLE',
  TOGGLE_VISIBLE_ALL: 'TOGGLE_VISIBLE_ALL',
}

export const reducer = (
  state,
  { type: actionType, payload: { name, detections } = {} },
) => {
  const module = state[name] || {}

  switch (actionType) {
    case ACTIONS.TOGGLE_OPEN: {
      if (state[name]?.isOpen === true) {
        return { ...state, [name]: { ...module, isOpen: false } }
      }

      return { ...state, [name]: { ...module, isOpen: true } }
    }

    case ACTIONS.TOGGLE_VISIBLE: {
      if (state[name]?.isVisible === false) {
        return { ...state, [name]: { ...module, isVisible: true } }
      }

      return { ...state, [name]: { ...module, isVisible: false } }
    }

    case ACTIONS.TOGGLE_VISIBLE_ALL: {
      const isAllVisible = Object.values(state).every(
        ({ isVisible }) => isVisible === true,
      )

      return Object.values(detections).reduce(
        (acc, { name: moduleName }) => ({
          ...acc,
          [moduleName]: { ...state[moduleName], isVisible: !isAllVisible },
        }),
        [],
      )
    }

    default:
      return state
  }
}

export const MIN_WIDTH = 200

export const ACTIONS = {
  UPDATE_FILTER: 'UPDATE_FILTER',
  TOGGLE_HIGHLIGHTS: 'TOGGLE_HIGHLIGHTS',
  RESIZE_MODULES: 'RESIZE_MODULES',
  UPDATE_TIMELINES: 'UPDATE_TIMELINES',
  TOGGLE_OPEN: 'TOGGLE_OPEN',
  TOGGLE_VISIBLE: 'TOGGLE_VISIBLE',
  TOGGLE_VISIBLE_ALL: 'TOGGLE_VISIBLE_ALL',
  INCREMENT: 'INCREMENT',
  DECREMENT: 'DECREMENT',
  ZOOM: 'ZOOM',
}

export const INITIAL_STATE = {
  filter: '',
  highlights: false,
  width: MIN_WIDTH,
  zoom: 100,
  timelines: {},
}

export const reducer = (
  state,
  { type: actionType, payload: { timeline, timelines, value } = {} },
) => {
  const module = state.timelines[timeline] || {}

  switch (actionType) {
    case ACTIONS.UPDATE_FILTER: {
      return { ...state, filter: value }
    }

    case ACTIONS.TOGGLE_HIGHLIGHTS: {
      return { ...state, highlights: !state.highlights }
    }

    case ACTIONS.RESIZE_MODULES: {
      return { ...state, width: value }
    }

    case ACTIONS.UPDATE_TIMELINES: {
      return { ...state, timelines: value }
    }

    case ACTIONS.TOGGLE_OPEN: {
      if (state.timelines[timeline]?.isOpen === true) {
        return {
          ...state,
          timelines: {
            ...state.timelines,
            [timeline]: { ...module, isOpen: false },
          },
        }
      }

      return {
        ...state,
        timelines: {
          ...state.timelines,
          [timeline]: { ...module, isOpen: true },
        },
      }
    }

    case ACTIONS.TOGGLE_VISIBLE: {
      if (state.timelines[timeline]?.isVisible === false) {
        return {
          ...state,
          timelines: {
            ...state.timelines,
            [timeline]: { ...module, isVisible: true },
          },
        }
      }

      return {
        ...state,
        timelines: {
          ...state.timelines,
          [timeline]: { ...module, isVisible: false },
        },
      }
    }

    case ACTIONS.TOGGLE_VISIBLE_ALL: {
      const isAllVisible = Object.values(state.timelines).every(
        ({ isVisible }) => isVisible === true,
      )

      return {
        ...state,
        timelines: Object.values(timelines).reduce(
          (acc, { timeline: moduleName }) => ({
            ...acc,
            [moduleName]: {
              ...state.timelines[moduleName],
              isVisible: !isAllVisible,
            },
          }),
          [],
        ),
      }
    }

    case ACTIONS.INCREMENT:
      return {
        ...state,
        zoom: value,
      }

    case ACTIONS.DECREMENT: {
      return {
        ...state,
        zoom: value < 100 ? 100 : value,
      }
    }

    case ACTIONS.ZOOM:
      return {
        ...state,
        zoom: value < 100 ? 100 : value,
      }

    default:
      return state
  }
}

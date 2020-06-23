import { v4 as uuidv4 } from 'uuid'

import FacetSvg from '../Icons/facet.svg'
import RangeSvg from '../Icons/range.svg'

const ICON_SIZE = 22

export const TYPES = [
  {
    type: 'facet',
    icon: <FacetSvg width={ICON_SIZE} />,
    name: 'Facet',
    legend:
      'Shows the range of values and the number of each for one for a selected field.',
  },
  {
    type: 'range',
    icon: <RangeSvg width={ICON_SIZE} />,
    name: 'Range',
    legend:
      'Shows the min, max, mean, median, and mode for the numerical values of a selected field.',
  },
]

export const ACTIONS = {
  CREATE: 'CREATE',
  UPDATE: 'UPDATE',
  DELETE: 'DELETE',
  CLEAR: 'CLEAR',
}

export const reducer = (state, { type: actionType, payload }) => {
  switch (actionType) {
    case 'CREATE': {
      const { type } = payload

      return [
        {
          id: uuidv4(),
          type,
        },
        ...state,
      ]
    }

    case 'UPDATE': {
      const { updatedChart, chartIndex } = payload

      return [
        ...state.slice(0, chartIndex),
        updatedChart,
        ...state.slice(chartIndex + 1),
      ]
    }

    case 'DELETE': {
      const { chartIndex } = payload

      return [...state.slice(0, chartIndex), ...state.slice(chartIndex + 1)]
    }

    case 'CLEAR':
      return []

    default:
      return state
  }
}

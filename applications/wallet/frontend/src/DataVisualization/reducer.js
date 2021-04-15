import { v4 as uuidv4 } from 'uuid'

import { constants } from '../Styles'

import FacetSvg from '../Icons/facet.svg'
import RangeSvg from '../Icons/range.svg'
import HistogramSvg from '../Icons/histogram.svg'

import { ENVS } from '../Feature'

export const TYPES = [
  {
    type: 'facet',
    icon: <FacetSvg height={constants.icons.moderate} />,
    legend:
      'Shows the range of values and the number of each for one for a selected field.',
  },
  {
    type: 'range',
    icon: <RangeSvg height={constants.icons.moderate} />,
    legend:
      'Shows the min, max, mean, median, and mode for the numerical values of a selected field.',
  },
  {
    type: 'histogram',
    icon: <HistogramSvg height={constants.icons.moderate} />,
    legend: 'Shows the distribution of assets within value groups.',
    flag: 'histogram-feature-flag',
    envs: [ENVS.QA],
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
      return [
        {
          id: uuidv4(),
          ...payload,
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

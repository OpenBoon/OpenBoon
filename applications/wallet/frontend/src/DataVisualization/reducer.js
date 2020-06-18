import FacetSvg from '../Icons/facet.svg'
import RangeSvg from '../Icons/range.svg'

const ICON_SIZE = 22

export const TYPES = [
  {
    type: 'FACET',
    icon: <FacetSvg width={ICON_SIZE} />,
    name: 'Facet',
    legend:
      'Shows the range of values and the number of each for one for a selected field.',
  },
  {
    type: 'RANGE',
    icon: <RangeSvg width={ICON_SIZE} />,
    name: 'Range',
    legend:
      'Shows the min, max, mean, median, and mode for the numerical values of a selected field.',
  },
]

export const ACTIONS = {
  CREATE: 'CREATE',
  CLEAR: 'CLEAR',
}

export const reducer = (state, action) => {
  switch (action.type) {
    case 'CREATE':
      return [...state, { type: action.payload.type }]

    case 'CLEAR':
      return []

    default:
      return state
  }
}

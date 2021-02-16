import PropTypes from 'prop-types'

import filterShape from './shape'

import { colors, constants } from '../Styles'

import CalendarSvg from '../Icons/calendar.svg'
import TextSvg from '../Icons/text.svg'
import SimilaritySvg from '../Icons/similarity.svg'
import RangeSvg from '../Icons/range.svg'
import ExistsSvg from '../Icons/exists.svg'
import MissingSvg from '../Icons/missing.svg'
import FacetSvg from '../Icons/facet.svg'

const HEIGHT = constants.icons.regular
const COLOR = colors.key.two

const FilterIcon = ({ filter: { type, values } }) => {
  switch (type) {
    case 'date': {
      return <CalendarSvg height={HEIGHT} color={COLOR} />
    }

    case 'textContent': {
      return <TextSvg height={HEIGHT} color={COLOR} />
    }

    case 'similarity': {
      return <SimilaritySvg height={HEIGHT} color={COLOR} />
    }

    case 'range': {
      return <RangeSvg height={HEIGHT} color={COLOR} />
    }

    case 'exists': {
      if (values && values.exists) {
        return <ExistsSvg height={HEIGHT} color={COLOR} />
      }

      return <MissingSvg height={HEIGHT} color={COLOR} />
    }

    case 'facet':
    case 'labelConfidence':
    case 'label':
    default: {
      return <FacetSvg height={HEIGHT} color={COLOR} />
    }
  }
}

FilterIcon.propTypes = {
  filter: PropTypes.shape(filterShape).isRequired,
}

export default FilterIcon

import PropTypes from 'prop-types'

import filterShape from './shape'

import { colors, constants, spacing, typography } from '../Styles'

import FacetSvg from '../Icons/facet.svg'
import RangeSvg from '../Icons/range.svg'
import ExistsSvg from '../Icons/exists.svg'
import MissingSvg from '../Icons/missing.svg'
import SimilaritySvg from '../Icons/similarity.svg'
import TextSvg from '../Icons/text.svg'
import CalendarSvg from '../Icons/calendar.svg'

const FilterTitle = ({ filter: { attribute, type, values } }) => {
  return (
    <>
      {(type === 'facet' || type === 'labelConfidence') && (
        <FacetSvg height={constants.icons.regular} color={colors.key.one} />
      )}

      {type === 'range' && (
        <RangeSvg height={constants.icons.regular} color={colors.key.one} />
      )}

      {type === 'exists' &&
        (values && values.exists ? (
          <ExistsSvg height={constants.icons.regular} color={colors.key.one} />
        ) : (
          <MissingSvg height={constants.icons.regular} color={colors.key.one} />
        ))}

      {type === 'similarity' && (
        <SimilaritySvg
          height={constants.icons.regular}
          color={colors.key.one}
        />
      )}

      {type === 'textContent' && (
        <TextSvg height={constants.icons.regular} color={colors.key.one} />
      )}

      {type === 'date' && (
        <CalendarSvg height={constants.icons.regular} color={colors.key.one} />
      )}

      <span
        css={{
          fontFamily: typography.family.mono,
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          paddingLeft: spacing.base,
          overflow: 'hidden',
          whiteSpace: 'nowrap',
          textOverflow: 'ellipsis',
        }}
      >
        {attribute}
      </span>
    </>
  )
}

FilterTitle.propTypes = {
  filter: PropTypes.shape(filterShape).isRequired,
}

export default FilterTitle

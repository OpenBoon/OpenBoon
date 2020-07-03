import PropTypes from 'prop-types'

import filterShape from './shape'

import { colors, spacing, typography } from '../Styles'

import FacetSvg from '../Icons/facet.svg'
import RangeSvg from '../Icons/range.svg'
import ExistsSvg from '../Icons/exists.svg'
import MissingSvg from '../Icons/missing.svg'
import SimilaritySvg from '../Icons/similarity.svg'
import TextSvg from '../Icons/text.svg'
import CalendarSvg from '../Icons/calendar.svg'

const ICON_SIZE = 20

const FilterTitle = ({ filter: { attribute, type, values } }) => {
  return (
    <>
      {(type === 'facet' || type === 'labelConfidence') && (
        <FacetSvg height={ICON_SIZE} color={colors.key.one} />
      )}

      {type === 'range' && (
        <RangeSvg height={ICON_SIZE} color={colors.key.one} />
      )}

      {type === 'exists' &&
        (values && values.exists ? (
          <ExistsSvg height={ICON_SIZE} color={colors.key.one} />
        ) : (
          <MissingSvg height={ICON_SIZE} color={colors.key.one} />
        ))}

      {type === 'similarity' && (
        <SimilaritySvg height={ICON_SIZE} color={colors.key.one} />
      )}

      {type === 'textContent' && (
        <TextSvg height={ICON_SIZE} color={colors.key.one} />
      )}

      {type === 'date' && (
        <CalendarSvg height={ICON_SIZE} color={colors.key.one} />
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

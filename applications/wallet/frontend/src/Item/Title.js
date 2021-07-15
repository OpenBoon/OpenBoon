import PropTypes from 'prop-types'

import { spacing, typography, colors } from '../Styles'

const ItemTitle = ({ type, name }) => {
  return (
    <div>
      <div
        css={{
          color: colors.structure.zinc,
          fontFamily: typography.family.condensed,
          textTransform: 'uppercase',
        }}
      >
        {type} Name:
      </div>

      <h3
        css={{
          color: colors.structure.white,
          fontWeight: typography.weight.medium,
          fontSize: typography.size.giant,
          lineHeight: typography.height.giant,
          paddingBottom: spacing.normal,
        }}
      >
        {name}
      </h3>
    </div>
  )
}

ItemTitle.propTypes = {
  type: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
}

export default ItemTitle

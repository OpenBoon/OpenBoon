import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

const MetadataPrettyNoResults = ({ name }) => {
  return (
    <>
      <div
        css={{
          '&:not(:first-of-type)': {
            borderTop: constants.borders.largeDivider,
          },
          padding: spacing.normal,
          paddingBottom: spacing.base,
          fontFamily: typography.family.mono,
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.white,
        }}
      >
        {name}
      </div>
      <div
        css={{
          width: '100%',
          padding: spacing.normal,
          paddingTop: spacing.base,
          wordBreak: 'break-word',
          color: colors.structure.zinc,
          fontStyle: typography.style.italic,
        }}
      >
        No Results
      </div>
    </>
  )
}

MetadataPrettyNoResults.propTypes = {
  name: PropTypes.string.isRequired,
}

export default MetadataPrettyNoResults

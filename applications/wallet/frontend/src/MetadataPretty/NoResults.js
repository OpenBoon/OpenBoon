import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

const MetadataPrettyNoResults = ({ name }) => {
  return (
    <>
      <div
        css={{
          '&:not(:first-of-type)': {
            borderTop: constants.borders.large.smoke,
          },
          padding: spacing.normal,
          paddingBottom: spacing.base,
          fontWeight: typography.weight.bold,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          color: colors.key.two,
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
  name: PropTypes.node.isRequired,
}

export default MetadataPrettyNoResults

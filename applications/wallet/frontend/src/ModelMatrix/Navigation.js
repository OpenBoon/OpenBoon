import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, constants, spacing, typography } from '../Styles'

import BackSvg from '../Icons/back.svg'

const ModelMatrixNavigation = ({ projectId, modelId, name }) => {
  return (
    <div
      css={{
        display: 'flex',
        padding: spacing.small,
        backgroundColor: colors.structure.coal,
      }}
    >
      <Link href={`/${projectId}/models/${modelId}`} passHref>
        <a
          css={{
            display: 'flex',
            alignItems: 'center',
            padding: spacing.base,
            color: colors.structure.zinc,
            fontSize: typography.size.medium,
            lineHeight: typography.height.medium,
            ':hover, &.focus-visible:focus': {
              textDecoration: 'none',
              color: colors.structure.white,
            },
          }}
        >
          <BackSvg
            height={constants.icons.regular}
            css={{ marginRight: spacing.base }}
          />
          Model Details: {name}
        </a>
      </Link>
    </div>
  )
}

ModelMatrixNavigation.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
}

export default ModelMatrixNavigation

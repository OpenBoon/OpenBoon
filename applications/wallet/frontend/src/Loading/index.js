import PropTypes from 'prop-types'

import { colors, typography, spacing, constants } from '../Styles'

import GeneratingSvg from '../Icons/generating.svg'

const MIN_HEIGHT = 300

const Loading = ({ isTransparent }) => {
  return (
    <div
      className="Loading"
      css={{
        minHeight: MIN_HEIGHT,
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        color: colors.structure.steel,
        backgroundColor: isTransparent
          ? colors.structure.transparent
          : colors.structure.lead,
        fontSize: typography.size.regular,
        lineHeight: typography.height.regular,
        boxShadow: isTransparent ? 'none' : constants.boxShadows.default,
        height: '100%',
      }}
    >
      <GeneratingSvg
        height={20}
        css={{
          animation: constants.animations.infiniteRotation,
          marginRight: spacing.normal,
        }}
      />
      Loading...
    </div>
  )
}

Loading.defaultProps = {
  isTransparent: false,
}

Loading.propTypes = {
  isTransparent: PropTypes.bool,
}

export default Loading

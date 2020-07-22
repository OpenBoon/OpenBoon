import PropTypes from 'prop-types'

import { colors, typography, spacing, constants } from '../Styles'

import GeneratingSvg from '../Icons/generating.svg'

const MIN_HEIGHT = 300

const Loading = ({ transparent }) => {
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
        backgroundColor: transparent
          ? colors.structure.transparent
          : colors.structure.lead,
        fontSize: typography.size.regular,
        lineHeight: typography.height.regular,
        boxShadow: transparent ? 'none' : constants.boxShadows.default,
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
  transparent: false,
}

Loading.propTypes = {
  transparent: PropTypes.bool,
}

export default Loading

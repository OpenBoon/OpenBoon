import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import HelpSvg from '../Icons/help.svg'

const ICON_SIZE = 20
const CARET_SIZE = 6
const CARET_POSITION = 14
const TEXTBOX_POSITION = 10

const ToggleTip = ({ openToThe, children }) => {
  return (
    <div
      css={{
        display: 'flex',
        position: 'relative',
        marginLeft: spacing.base,
        ':hover, :focus-within': {
          div: {
            visibility: 'visible',
            opacity: 1,
            transition: 'all 0.5s ease 0.25s',
          },
          button: {
            color: colors.structure.white,
          },
        },
      }}
    >
      <button
        aria-label="Training Help"
        aria-details="trainingHelpText"
        type="button"
        css={{
          display: 'flex',
          border: 0,
          padding: 0,
          backgroundColor: colors.structure.transparent,
          color: colors.structure.steel,
        }}
      >
        <HelpSvg height={ICON_SIZE} />
      </button>

      <div
        role="tooltip"
        id="trainingHelpText"
        css={{
          position: 'absolute',
          [openToThe === 'left' ? 'right' : 'left']: -TEXTBOX_POSITION,
          top: ICON_SIZE + spacing.base,
          color: colors.structure.coal,
          backgroundColor: colors.structure.white,
          borderRadius: constants.borderRadius.small,
          padding: spacing.moderate,
          width: 'max-content',
          maxWidth: constants.toggleTip.maxWidth,
          visibility: 'hidden',
          opacity: 0,
          transition: 'all 0.5s ease 0.25s',
          ':hover': {
            visibility: 'visible',
            opacity: 1,
          },
        }}
      >
        <div
          css={{
            '&:before': {
              content: `' '`,
              position: 'absolute',
              top: -CARET_SIZE,
              [openToThe === 'left' ? 'right' : 'left']: CARET_POSITION,
              borderBottom: `${CARET_SIZE}px solid ${colors.structure.white}`,
              borderLeft: `${CARET_SIZE}px solid transparent`,
              borderRight: `${CARET_SIZE}px solid transparent`,
            },
          }}
        />
        {children}
      </div>
    </div>
  )
}

ToggleTip.propTypes = {
  openToThe: PropTypes.oneOf(['left', 'right']).isRequired,
  children: PropTypes.node.isRequired,
}

export default ToggleTip
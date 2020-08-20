import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import HelpSvg from '../Icons/help.svg'

const CARET_SIZE = 6
const CARET_POSITION = 14
const TEXTBOX_POSITION = 10
const MAX_WIDTH = 400

const Toggletip = ({ openToThe, label, children }) => {
  const id = label.replace(' ', '')

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
        aria-label={label}
        aria-details={id}
        type="button"
        css={{
          display: 'flex',
          border: 0,
          padding: 0,
          backgroundColor: colors.structure.transparent,
          color: colors.structure.steel,
        }}
      >
        <HelpSvg height={constants.icons.regular} />
      </button>

      <div
        role="tooltip"
        id={id}
        css={{
          position: 'absolute',
          [openToThe === 'left' ? 'right' : 'left']: -TEXTBOX_POSITION,
          top: constants.icons.regular + spacing.base,
          color: colors.structure.coal,
          backgroundColor: colors.structure.white,
          borderRadius: constants.borderRadius.small,
          boxShadow: constants.boxShadows.default,
          padding: spacing.moderate,
          width: 'max-content',
          maxWidth: MAX_WIDTH,
          visibility: 'hidden',
          opacity: 0,
          transition: 'all 0.5s ease 0.25s',
          zIndex: zIndex.reset,
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

Toggletip.propTypes = {
  openToThe: PropTypes.oneOf(['left', 'right']).isRequired,
  label: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default Toggletip

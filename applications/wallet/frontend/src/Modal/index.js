import PropTypes from 'prop-types'
import AriaModal from 'react-aria-modal'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import CrossSvg from '../Icons/cross.svg'

const BUTTON_HEIGHT = 40
const MODAL_WIDTH = 480

const Modal = ({ isPrimary, title, message, action, onCancel, onConfirm }) => {
  return (
    <AriaModal
      titleId={title}
      getApplicationNode={() => document.getElementById('__next')}
      underlayColor="rgba(0, 0, 0, 0.75)"
      onExit={onCancel}
      verticallyCenter
    >
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          width: MODAL_WIDTH,
          borderRadius: constants.borderRadius.small,
          overflow: 'hidden',
          boxShadow: constants.boxShadows.modal,
        }}
      >
        <header
          css={{
            backgroundColor: colors.structure.coal,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: spacing.normal,
            textTransform: 'uppercase',
          }}
        >
          <div css={{ color: colors.structure.zinc }}>{title}</div>
          <div
            role="button"
            aria-label="Close Modal"
            tabIndex="-1"
            onClick={onCancel}
            onKeyDown={onCancel}
          >
            <CrossSvg
              height={constants.icons.regular}
              color={colors.structure.white}
            />
          </div>
        </header>

        <div
          css={{
            height: '100%',
            backgroundColor: colors.structure.iron,
            padding: spacing.spacious,
          }}
        >
          <div
            css={{
              display: 'flex',
              alignItems: 'center',
              color: colors.marble,
            }}
          >
            {message}
          </div>

          <div
            css={{
              display: 'flex',
              justifyContent: 'center',
              paddingTop: spacing.spacious,
              button: {
                height: BUTTON_HEIGHT,
                padding: `0 ${spacing.spacious}px`,
                border: 'none',
                borderRadius: constants.borderRadius.small,
              },
            }}
          >
            <Button
              variant={VARIANTS.SECONDARY}
              css={{
                marginRight: spacing.normal,
              }}
              onClick={onCancel}
            >
              Cancel
            </Button>

            <Button
              variant={isPrimary ? VARIANTS.PRIMARY : VARIANTS.WARNING}
              onClick={onConfirm}
              isDisabled={action.includes('...')}
            >
              {action}
            </Button>
          </div>
        </div>
      </div>
    </AriaModal>
  )
}

Modal.defaultProps = {
  isPrimary: false,
}

Modal.propTypes = {
  isPrimary: PropTypes.bool,
  title: PropTypes.string.isRequired,
  message: PropTypes.string.isRequired,
  action: PropTypes.string.isRequired,
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
}

export default Modal

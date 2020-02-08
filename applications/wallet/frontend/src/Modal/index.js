import PropTypes from 'prop-types'
import AriaModal from 'react-aria-modal'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import CrossSvg from '../Icons/cross.svg'

const BUTTON_HEIGHT = 40
const MODAL_HEIGHT = 200
const MODAL_WIDTH = 480

const Modal = ({ onCancel, onConfirm }) => {
  return (
    <AriaModal
      titleId="Delete API Key"
      getApplicationNode={() => document.getElementById('__next')}
      underlayColor="rgba(0, 0, 0, 0.75)"
      onExit={onCancel}
      verticallyCenter>
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          width: MODAL_WIDTH,
          height: MODAL_HEIGHT,
        }}>
        <header
          css={{
            backgroundColor: colors.structure.black,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: spacing.normal,
            textTransform: 'uppercase',
          }}>
          <div css={{ color: colors.structure.zinc }}>Delete</div>
          <div
            role="button"
            aria-label="Close Modal"
            tabIndex="-1"
            onClick={onCancel}
            onKeyDown={onCancel}>
            <CrossSvg width={20} color={colors.structure.white} />
          </div>
        </header>

        <div
          css={{
            height: '100%',
            backgroundColor: colors.structure.iron,
            padding: spacing.spacious,
          }}>
          <div
            css={{
              display: 'flex',
              alignItems: 'center',
              color: colors.marble,
            }}>
            Deleting this key cannot be undone.
          </div>
          <div
            css={{
              display: 'flex',
              justifyContent: 'flex-end',
              paddingTop: spacing.spacious,
              button: {
                height: BUTTON_HEIGHT,
                padding: `0 ${spacing.spacious}px`,
                border: 'none',
                borderRadius: constants.borderRadius.small,
              },
            }}>
            <Button
              variant={VARIANTS.SECONDARY}
              css={{
                marginRight: spacing.normal,
              }}
              onClick={onCancel}>
              Cancel
            </Button>
            <Button variant={VARIANTS.WARNING} onClick={onConfirm}>
              Delete Permanently
            </Button>
          </div>
        </div>
      </div>
    </AriaModal>
  )
}

Modal.propTypes = {
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
}

export default Modal

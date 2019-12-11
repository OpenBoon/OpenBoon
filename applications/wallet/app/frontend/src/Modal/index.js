import React from 'react'
import AriaModal from 'react-aria-modal'
import WarningSvg from '../Icons/warning.svg'
import XSvg from '../Icons/x.svg'
import { colors, constants, spacing } from '../Styles'

const BUTTON_HEIGHT = 40

const Modal = () => {
  const [showModal, setShowModal] = React.useState(false)
  const open = () => setShowModal(true)
  const close = () => setShowModal(false)

  return (
    <div>
      <button type="button" onClick={open}>
        Open Modal
      </button>

      {showModal && (
        <AriaModal
          titleId="demo-two-title"
          onExit={close}
          underlayClickExits={true}
          verticallyCenter={true}>
          <div
            css={{
              display: 'flex',
              flexDirection: 'column',
              width: 480,
              height: 204,
            }}>
            <header
              css={{
                backgroundColor: colors.grey4,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: spacing.normal,
              }}>
              <div css={{ color: colors.rocks.steel }}>DELETE</div>
              <div
                role="button"
                aria-label="Close Modal"
                tabIndex="-1"
                onClick={close}
                onKeyDown={close}>
                <XSvg width={20} color={colors.rocks.steel} />
              </div>
            </header>

            <div
              css={{
                height: '100%',
                backgroundColor: colors.grey1,
                padding: spacing.spacious,
              }}>
              <div
                css={{
                  display: 'flex',
                  alignItems: 'center',
                  color: colors.marble,
                }}>
                <WarningSvg width={20} color={colors.warning} />
                <div css={{ paddingLeft: spacing.base }}>
                  Deleting this key cannot be undone.
                </div>
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
                <button
                  type="button"
                  css={{
                    backgroundColor: colors.rocks.iron,
                    color: colors.rocks.granite,
                    marginRight: spacing.normal,
                  }}
                  onClick={close}>
                  Cancel
                </button>
                <button
                  type="button"
                  css={{
                    backgroundColor: colors.warning,
                    color: colors.primaryFont,
                  }}
                  onClick={close}>
                  Delete Permanently
                </button>
              </div>
            </div>
          </div>
        </AriaModal>
      )}
    </div>
  )
}

export default Modal

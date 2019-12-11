import React from 'react'
import WarningSvg from '../Icons/warning.svg'
import XSvg from '../Icons/x.svg'
import { colors, constants, spacing } from '../Styles'

const BUTTON_HEIGHT = 40

const Modal = () => {
  const [showDialog, setShowDialog] = React.useState(false)
  const open = () => setShowDialog(true)
  const close = () => setShowDialog(false)

  return (
    <div>
      <button type="button" onClick={open}>
        Open Modal
      </button>

      {showDialog && (
        <div
          role="button"
          aria-label="Close Modal Overlay"
          tabIndex="-1"
          css={{
            position: 'fixed',
            top: 0,
            left: 0,
            bottom: 0,
            width: '100%',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            background: 'hsla(0, 100%, 0%, 0.75)',
          }}
          onClick={close}
          onKeyDown={close}>
          <div
            css={{
              display: 'flex',
              flexDirection: 'column',
              width: 480,
              height: 204,
            }}>
            <div
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
            </div>

            <div
              css={{
                height: '100%',
                backgroundColor: colors.grey1,
                padding: spacing.spacious,
              }}>
              <div
                css={{
                  display: 'flex',
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
        </div>
      )}
    </div>
  )
}

export default Modal

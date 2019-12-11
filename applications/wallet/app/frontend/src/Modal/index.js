import React from 'react'
import XSvg from '../Icons/x.svg'
import { colors, constants, spacing, typography } from '../Styles'

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
                height: 40,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: `${spacing.moderate}px ${spacing.normal}px`,
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
              }}>
              <div
                css={{
                  padding: spacing.base,
                  color: colors.marble,
                  fontSize: typography.size.hecto,
                  lineHeight: typography.height.hecto,
                }}>
                Deleting this key cannot be undone.
              </div>
              <div
                css={{
                  paddingLeft: 124,
                  paddingRight: 32,
                  display: 'flex',
                  justifyContent: 'space-between',
                  button: {
                    height: 40,
                    width: 152,
                    border: 'none',
                    borderRadius: constants.borderRadius.small,
                    fontSize: typography.size.hecto,
                    lineHeight: typography.height.hecto,
                  },
                }}>
                <button
                  type="button"
                  css={{
                    backgroundColor: colors.rocks.iron,
                    color: colors.rocks.granite,
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

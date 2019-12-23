import { useState } from 'react'
import AriaModal from 'react-aria-modal'
import WarningSvg from '../Icons/warning.svg'
import CrossSvg from '../Icons/cross.svg'
import { colors, constants, spacing } from '../Styles'

const BUTTON_HEIGHT = 40
const MODAL_HEIGHT = 200
const MODAL_WIDTH = 480

const Modal = () => {
  const [showModal, setShowModal] = useState(false)

  return (
    <div>
      <button
        type="button"
        onClick={() => {
          setShowModal(true)
        }}>
        Open Modal
      </button>

      {showModal && (
        <AriaModal
          titleId="Modal"
          onExit={() => {
            setShowModal(false)
          }}
          underlayClickExits
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
                backgroundColor: colors.grey4,
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
                onClick={() => {
                  setShowModal(false)
                }}
                onKeyDown={() => {
                  setShowModal(false)
                }}>
                <CrossSvg width={20} color={colors.structure.zinc} />
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
                    backgroundColor: colors.structure.steel,
                    color: colors.rocks.granite,
                    marginRight: spacing.normal,
                  }}
                  onClick={() => {
                    setShowModal(false)
                  }}>
                  Cancel
                </button>
                <button
                  type="button"
                  css={{
                    backgroundColor: colors.warning,
                    color: colors.primaryFont,
                  }}
                  onClick={() => {
                    setShowModal(false)
                  }}>
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

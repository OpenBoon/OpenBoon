import { useState } from 'react'
import AriaModal from 'react-aria-modal'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import WarningSvg from '../Icons/warning.svg'
import CrossSvg from '../Icons/cross.svg'

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
                backgroundColor: colors.structure.iron,
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
                backgroundColor: colors.structure.mattGrey,
                padding: spacing.spacious,
              }}>
              <div
                css={{
                  display: 'flex',
                  alignItems: 'center',
                  color: colors.marble,
                }}>
                <WarningSvg width={20} color={colors.signal.warning.base} />
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
                <Button
                  type="button"
                  variant={VARIANTS.SECONDARY}
                  css={{
                    marginRight: spacing.normal,
                  }}
                  onClick={() => {
                    setShowModal(false)
                  }}>
                  Cancel
                </Button>
                <Button
                  type="button"
                  variant={VARIANTS.WARNING}
                  onClick={() => {
                    setShowModal(false)
                  }}>
                  Delete Permanently
                </Button>
              </div>
            </div>
          </div>
        </AriaModal>
      )}
    </div>
  )
}

export default Modal

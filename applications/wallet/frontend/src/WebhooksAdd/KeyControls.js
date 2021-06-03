import { useEffect } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonCopy from '../Button/Copy'

import { generateSecretKey } from './helpers'

const WebhooksAddKeyControls = ({ state, dispatch }) => {
  useEffect(() => {
    if (state.isCopied) {
      const id = setTimeout(() => {
        dispatch({ isCopied: false })
      }, 3000)

      return () => {
        clearTimeout(id)
      }
    }
    return null
  }, [state.isCopied, dispatch])

  return (
    <div
      css={{
        position: 'absolute',
        left: '100%',
        paddingLeft: spacing.base,
        paddingTop: spacing.base,
        display: 'flex',
        alignItems: 'center',
      }}
    >
      <ButtonCopy title="Secret Key" value={state.secretKey} offset={50} />

      <span css={{ padding: spacing.small, color: colors.structure.steel }}>
        |
      </span>

      <div css={{ position: 'relative' }}>
        <Button
          variant={BUTTON_VARIANTS.ICON}
          isDisabled={state.disableSecretKeyButton}
          onClick={generateSecretKey({ state, dispatch })}
        >
          Generate Key
        </Button>

        {state.isCopied && (
          <div
            css={{
              position: 'absolute',
              left: '50%',
            }}
          >
            <div
              css={{
                marginLeft: '-50%',
                marginRight: '50%',
                whiteSpace: 'nowrap',
                backgroundColor: colors.structure.white,
                color: colors.structure.coal,
                padding: spacing.small,
                display: 'flex',
                borderRadius: constants.borderRadius.small,
                boxShadow: constants.boxShadows.default,
              }}
            >
              <CheckmarkSvg
                height={constants.icons.regular}
                color={colors.signal.grass.base}
              />
              Secret Key Copied to Clipboard!
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

WebhooksAddKeyControls.propTypes = {
  state: PropTypes.shape({
    disableSecretKeyButton: PropTypes.bool.isRequired,
    isCopied: PropTypes.bool.isRequired,
    secretKey: PropTypes.string.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default WebhooksAddKeyControls

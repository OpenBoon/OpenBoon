import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import CopySvg from '../Icons/copy.svg'
import CheckmarkSvg from '../Icons/checkmark.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '.'

const ButtonCopy = ({ title, value, offset }) => {
  const [isCopied, setCopied] = useState(false)

  /* istanbul ignore next */
  useEffect(() => {
    if (isCopied) {
      const id = setTimeout(() => {
        setCopied(false)
      }, 3000)

      return () => {
        clearTimeout(id)
      }
    }
    return null
  }, [isCopied])

  return (
    <div css={{ position: 'relative' }}>
      <Button
        title={`Copy ${title} to Clipboard`}
        aria-label={`Copy ${title} to Clipboard`}
        variant={BUTTON_VARIANTS.ICON}
        onClick={async () => {
          await navigator.clipboard.writeText(value)

          setCopied(true)
        }}
      >
        <CopySvg height={constants.icons.regular} />
      </Button>

      {isCopied && (
        <div
          css={{
            position: 'absolute',
            left: `${offset}%`,
          }}
        >
          <div
            css={{
              marginLeft: `-${offset}%`,
              marginRight: `${offset}%`,
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
              css={{ opacity: '1 !important' }}
            />
            {title} Copied to Clipboard!
          </div>
        </div>
      )}
    </div>
  )
}

ButtonCopy.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  offset: PropTypes.number.isRequired,
}

export default ButtonCopy

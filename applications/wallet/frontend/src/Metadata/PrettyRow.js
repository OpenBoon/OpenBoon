import PropTypes from 'prop-types'
import useClipboard from 'react-use-clipboard'

import { colors, constants, spacing } from '../Styles'

import CopySvg from '../Icons/copy.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { formatDisplayName, formatDisplayValue } from './helpers'

const COPY_WIDTH = 20

const MetadataPrettyRow = ({ name, value, path }) => {
  const [isCopied, setCopied] = useClipboard(value, { successDuration: 1000 })

  if (typeof value === 'object') {
    return (
      <>
        <div
          css={{
            borderTop: constants.borders.divider,
            ':hover': {
              div: {
                svg: {
                  display: 'inline-block',
                },
              },
            },
          }}
        >
          <div
            css={{
              fontFamily: 'Roboto Condensed',
              color: colors.structure.steel,
              padding: spacing.normal,
              paddingBottom: 0,
              flex: 1,
            }}
          >
            <span title={`${path.toLowerCase()}.${name}`}>
              {formatDisplayName({ name })}
            </span>
          </div>
          <div />
        </div>

        <div
          css={{ paddingLeft: spacing.normal, paddingRight: spacing.normal }}
        >
          {Object.entries(value).map(([k, v], i) => (
            <MetadataPrettyRow
              key={k}
              name={k}
              value={v}
              path={`${path.toLowerCase()}.${name}`}
              index={i}
            />
          ))}
        </div>
      </>
    )
  }

  return (
    <div
      css={{
        display: 'flex',
        '&:not(:first-of-type)': {
          borderTop: constants.borders.divider,
        },
        ':hover': {
          backgroundColor: colors.signal.electricBlue.background,
          div: {
            color: colors.structure.white,
            svg: {
              display: 'inline-block',
            },
          },
        },
      }}
    >
      <div
        css={{
          fontFamily: 'Roboto Condensed',
          color: colors.structure.steel,
          padding: spacing.normal,
          flex: 1,
        }}
      >
        <span title={`${path.toLowerCase()}.${name}`}>
          {formatDisplayName({ name })}
        </span>
      </div>
      <div
        title={value}
        css={{
          flex: 4,
          fontFamily: 'Roboto Mono',
          color: colors.structure.pebble,
          padding: spacing.normal,
          wordBreak: name === 'content' ? 'break-word' : 'break-all',
        }}
      >
        {formatDisplayValue({ name, value })}
      </div>
      <div
        css={{
          width: COPY_WIDTH + spacing.normal,
          paddingTop: spacing.normal,
          paddingRight: spacing.normal,
        }}
      >
        <Button
          title="Copy to Clipboard"
          variant={BUTTON_VARIANTS.NEUTRAL}
          onClick={setCopied}
          isDisabled={isCopied}
        >
          <CopySvg
            width={COPY_WIDTH}
            color={colors.structure.steel}
            css={{
              display: 'none',
              ':hover': {
                color: colors.structure.white,
              },
            }}
          />
        </Button>
      </div>
    </div>
  )
}

MetadataPrettyRow.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.shape({}),
  ]).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettyRow

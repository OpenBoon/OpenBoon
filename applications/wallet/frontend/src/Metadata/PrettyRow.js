import PropTypes from 'prop-types'
import useClipboard from 'react-use-clipboard'

import { colors, constants, spacing } from '../Styles'

import CopySvg from '../Icons/copy.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { formatDisplayName, formatDisplayValue } from './helpers'

const COPY_WIDTH = 20

const MetadataPrettyRow = ({ name, value, title, index, indentation }) => {
  const [isCopied, setCopied] = useClipboard(value, { successDuration: 1000 })

  if (typeof value === 'object') {
    return (
      <>
        <tr
          css={{
            borderTop: index !== 0 ? constants.borders.divider : '',
            ':hover': {
              backgroundColor: colors.signal.electricBlue.background,
              td: {
                color: colors.structure.white,
                svg: {
                  display: 'inline-block',
                },
              },
            },
          }}
        >
          <td
            valign="top"
            css={{
              fontFamily: 'Roboto Condensed',
              color: colors.structure.steel,
              padding: spacing.normal,
            }}
          >
            <span title={`${title.toLowerCase()}.${name}`}>
              {formatDisplayName({ name })}
            </span>
          </td>
          <td />
          <td />
        </tr>

        {Object.entries(value).map(([k, v], i) => (
          <MetadataPrettyRow
            key={k}
            name={k}
            value={v}
            title={title}
            index={i}
            indentation={indentation + 1}
          />
        ))}
      </>
    )
  }

  return (
    <tr
      css={{
        borderTop: index !== 0 ? constants.borders.divider : '',
        ':hover': {
          backgroundColor: colors.signal.electricBlue.background,
          td: {
            color: colors.structure.white,
            svg: {
              display: 'inline-block',
            },
          },
        },
      }}
    >
      <td
        valign="top"
        css={{
          paddingLeft: indentation * spacing.normal,
          fontFamily: 'Roboto Condensed',
          color: colors.structure.steel,
          padding: spacing.normal,
        }}
      >
        <span title={`${title.toLowerCase()}.${name}`}>
          {formatDisplayName({ name })}
        </span>
      </td>
      <td
        valign="top"
        title={value}
        css={{
          fontFamily: 'Roboto Mono',
          color: colors.structure.pebble,
          padding: spacing.normal,
          wordBreak: name === 'content' ? 'break-word' : 'break-all',
        }}
      >
        {formatDisplayValue({ name, value })}
      </td>
      <td
        valign="top"
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
      </td>
    </tr>
  )
}

MetadataPrettyRow.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  title: PropTypes.string.isRequired,
  index: PropTypes.number.isRequired,
  indentation: PropTypes.number.isRequired,
}

export default MetadataPrettyRow

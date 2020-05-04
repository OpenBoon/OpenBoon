import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'
import MetadataBbox from './Bbox'
import MetadataLabelDetection from './LabelDetection'
import MetadataContentDetection from './ContentDetection'
import MetadataSimilarityDetection from './SimilarityDetection'

import { formatDisplayName, formatDisplayValue } from './helpers'

const MetadataPrettyRow = ({ name, value, path }) => {
  if (typeof value === 'object') {
    switch (value.type) {
      case 'labels':
        return Object.keys(value.predictions[0]).includes('bbox') ? (
          <MetadataBbox name={name} />
        ) : (
          <MetadataLabelDetection name={name} predictions={value.predictions} />
        )
      case 'content':
        return <MetadataContentDetection name={name} content={value.content} />
      case 'similarity':
        return (
          <MetadataSimilarityDetection name={name} simhash={value.simhash} />
        )
      default:
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
              css={{
                paddingLeft: spacing.normal,
                paddingRight: spacing.normal,
              }}
            >
              {Object.entries(value).map(([k, v]) => (
                <MetadataPrettyRow
                  key={k}
                  name={k}
                  value={v}
                  path={`${path.toLowerCase()}.${name}`}
                />
              ))}
            </div>
          </>
        )
    }
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
          width: COPY_SIZE + spacing.normal,
          paddingTop: spacing.normal,
          paddingRight: spacing.normal,
        }}
      >
        <ButtonCopy value={value} />
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

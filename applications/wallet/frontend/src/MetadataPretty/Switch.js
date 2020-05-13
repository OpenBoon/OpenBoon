import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import { formatDisplayName } from '../Metadata/helpers'

import MetadataPrettyLabels from './Labels'
import MetadataPrettyContent from './Content'
import MetadataPrettySimilarity from './Similarity'
import MetadataPrettyRow from './Row'

const MetadataPrettySwitch = ({ name, value, path }) => {
  if (Array.isArray(value)) {
    return value.map((attribute, index) => (
      <div
        // eslint-disable-next-line react/no-array-index-key
        key={`${path}${index}`}
        css={{
          width: '100%',
          '&:not(:first-of-type)': {
            borderTop: constants.borders.prettyMetadata,
          },
        }}
      >
        {Object.entries(attribute).map(([k, v]) => (
          <MetadataPrettySwitch key={k} name={k} value={v} path={path} />
        ))}
      </div>
    ))
  }

  if (typeof value === 'object') {
    switch (value.type) {
      case 'labels':
        return <MetadataPrettyLabels name={name} value={value} />

      case 'content':
        return <MetadataPrettyContent name={name} value={value} />

      case 'similarity':
        return <MetadataPrettySimilarity name={name} value={value} />

      default:
        return (
          <>
            {!!name && (
              <div
                css={{
                  borderTop: constants.borders.divider,
                  ':hover': { div: { svg: { display: 'inline-block' } } },
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
            )}

            <div
              css={
                name
                  ? {
                      paddingLeft: spacing.normal,
                      paddingRight: spacing.normal,
                    }
                  : {}
              }
            >
              {Object.entries(value).map(([k, v]) => (
                <MetadataPrettySwitch
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

  return <MetadataPrettyRow name={name} value={value} path={path} />
}

MetadataPrettySwitch.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.shape({}),
    PropTypes.array,
  ]).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettySwitch

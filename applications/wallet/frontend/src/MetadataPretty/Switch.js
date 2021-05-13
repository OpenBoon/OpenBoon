import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import { formatDisplayName } from '../Metadata/helpers'

import SuspenseBoundary from '../SuspenseBoundary'

import MetadataPrettyPredictions from './Predictions'
import MetadataPrettyContent from './Content'
import MetadataPrettySimilarity from './Similarity'
import MetadataPrettyRow from './Row'

const MetadataPrettySwitch = ({ path, name, value }) => {
  // Sort Analysis modules alphabetically
  const sortedValue =
    path === 'analysis'
      ? Object.keys(value)
          .sort()
          .reduce((obj, key) => ({ ...obj, [key]: value[key] }), {})
      : value

  if (!name && Array.isArray(sortedValue)) {
    return sortedValue.map((attribute, index) => (
      <div
        // eslint-disable-next-line react/no-array-index-key
        key={`${path}${index}`}
        css={{
          width: '100%',
          '&:not(:first-of-type)': {
            borderTop: constants.borders.large.iron,
          },
        }}
      >
        {Object.entries(attribute).map(([k, v]) => (
          <MetadataPrettySwitch key={k} path={path} name={k} value={v} />
        ))}
      </div>
    ))
  }

  if (typeof sortedValue === 'object') {
    switch (sortedValue.type) {
      case 'labels':
        return (
          <MetadataPrettyPredictions
            path={path}
            name={name}
            value={sortedValue}
          />
        )

      case 'content':
        return (
          <MetadataPrettyContent path={path} name={name} value={sortedValue} />
        )

      case 'similarity':
        return (
          <div
            css={{
              '&:not(:first-of-type)': {
                borderTop: constants.borders.large.smoke,
              },
            }}
          >
            <SuspenseBoundary isTransparent>
              <MetadataPrettySimilarity
                path={path}
                name={name}
                value={sortedValue}
              />
            </SuspenseBoundary>
          </div>
        )

      default:
        return (
          <>
            {!!name && (
              <div
                css={{
                  '&:not(:first-of-type)': {
                    borderTop: constants.borders.regular.smoke,
                  },
                }}
              >
                <div
                  css={{
                    fontFamily: typography.family.condensed,
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

            <div css={name ? { paddingLeft: spacing.normal } : {}}>
              {Object.entries(sortedValue).map(([k, v]) => (
                <MetadataPrettySwitch
                  key={k}
                  path={`${path.toLowerCase()}${name ? '.' : ''}${name}`}
                  name={k}
                  value={v}
                />
              ))}
              {Object.entries(sortedValue).length === 0 && (
                <MetadataPrettyRow path={path} name="" value="" />
              )}
            </div>
          </>
        )
    }
  }

  return <MetadataPrettyRow path={path} name={name} value={sortedValue} />
}

MetadataPrettySwitch.propTypes = {
  path: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.shape({}),
    PropTypes.array,
  ]).isRequired,
}

export default MetadataPrettySwitch

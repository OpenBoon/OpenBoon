import PropTypes from 'prop-types'

import { constants, spacing } from '../Styles'

import MetadataPrettyRow from './PrettyRow'

const MetadataPretty = ({ metadata, section }) => {
  if (section === 'metrics') {
    return metadata.metrics.pipeline.map((pipeline, index) => {
      const { processor, ...filteredPipeline } = pipeline

      return (
        <div
          // eslint-disable-next-line react/no-array-index-key
          key={`${section}${index}`}
          css={{
            width: '100%',
            '&:not(:first-of-type)': {
              borderTop: constants.borders.prettyMetadata,
            },
          }}
        >
          <div
            css={{ fontFamily: 'Roboto Condensed', padding: spacing.normal }}
          >
            PROCESSOR
            <div css={{ paddingTop: spacing.base, fontFamily: 'Roboto Mono' }}>
              {processor}
            </div>
          </div>
          {Object.entries(filteredPipeline).map(([key, value]) => (
            <MetadataPrettyRow
              key={key}
              name={key}
              value={value}
              path="metrics.pipeline"
            />
          ))}
        </div>
      )
    })
  }

  if (Array.isArray(metadata[section])) {
    return metadata[section].map((file, index) => (
      <div
        // eslint-disable-next-line react/no-array-index-key
        key={`${section}${index}`}
        css={{
          width: '100%',
          '&:not(:first-of-type)': {
            borderTop: constants.borders.prettyMetadata,
          },
        }}
      >
        {Object.entries(file).map(([key, value]) => (
          <MetadataPrettyRow
            key={key}
            name={key}
            value={value}
            path={section}
          />
        ))}
      </div>
    ))
  }

  return (
    <div css={{ width: '100%' }}>
      <div>
        {Object.entries(metadata[section]).map(([key, value]) => {
          return (
            <MetadataPrettyRow
              key={key}
              name={key}
              value={value}
              path={section}
            />
          )
        })}
      </div>
    </div>
  )
}

MetadataPretty.propTypes = {
  metadata: PropTypes.shape({
    source: PropTypes.shape({
      path: PropTypes.string,
      filename: PropTypes.string,
      extension: PropTypes.string,
      mimetype: PropTypes.string,
    }),
    system: PropTypes.shape({
      projectId: PropTypes.string,
      dataSourceId: PropTypes.string,
      jobId: PropTypes.string,
      taskId: PropTypes.string,
      timeCreated: PropTypes.string,
      state: PropTypes.string,
    }),
    files: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.string,
        size: PropTypes.number,
        name: PropTypes.string,
        mimetype: PropTypes.string,
        category: PropTypes.oneOf(['proxy', 'source', 'web-proxy']),
        attrs: PropTypes.shape({
          width: PropTypes.number,
          height: PropTypes.number,
        }),
      }),
    ),
    metrics: PropTypes.shape({
      pipeline: PropTypes.arrayOf(PropTypes.shape({})),
    }),
  }).isRequired,
  section: PropTypes.string.isRequired,
}

export default MetadataPretty

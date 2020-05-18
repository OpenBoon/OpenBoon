import PropTypes from 'prop-types'

import { constants, spacing, typography } from '../Styles'

import MetadataPrettySwitch from '../MetadataPretty/Switch'

import MetricsBar from './Bar'

const Metrics = ({ pipeline }) => {
  return (
    <>
      <div css={{ padding: spacing.normal, paddingBottom: spacing.comfy }}>
        <div css={{ paddingBottom: spacing.moderate }}>
          Pipelines: {pipeline.length}
        </div>
        <MetricsBar pipeline={pipeline} />
      </div>
      {pipeline.map((p) => {
        const { processor, ...filteredPipeline } = p

        return (
          <div
            key={processor}
            css={{
              width: '100%',
              '&:not(:first-of-type)': {
                borderTop: constants.borders.prettyMetadata,
              },
            }}
          >
            <div
              css={{
                fontFamily: 'Roboto Condensed',
                padding: spacing.normal,
              }}
            >
              PROCESSOR
              <div
                title={processor}
                css={{
                  paddingTop: spacing.base,
                  fontFamily: 'Roboto Mono',
                  fontSize: typography.size.small,
                  lineHeight: typography.height.small,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {processor}
              </div>
            </div>
            {Object.entries(filteredPipeline).map(([key, value]) => (
              <MetadataPrettySwitch
                key={key}
                name={key}
                value={value}
                path="metrics.pipeline"
              />
            ))}
          </div>
        )
      })}
    </>
  )
}

Metrics.propTypes = {
  pipeline: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
}

export default Metrics

import { useRouter } from 'next/router'

import { colors, constants, spacing } from '../Styles'

import TimelineAccordion, { COLOR_TAB_WIDTH } from './Accordion'

const COLORS = [
  colors.signal.sky.base,
  colors.graph.magenta,
  colors.signal.halloween.base,
  colors.signal.canary.base,
  colors.graph.seafoam,
  colors.graph.rust,
  colors.graph.coral,
  colors.graph.iris,
  colors.graph.marigold,
  colors.graph.magenta,
  colors.signal.grass.base,
]

// TODO: fetch modules from backend
const TIMELINE_MODULES = [
  {
    name: 'gcp-video-explicit-detection',
    predictions: [
      { label: 'Gh st', count: 3 },
      { label: 'Busters', count: 3 },
    ],
  },
  {
    name: 'gcp-video-label-detection',
    predictions: [
      { label: 'Label 1', count: 3 },
      { label: 'Label 2', count: 6 },
      { label: 'Label 3 Plus More Text to Make A Long Label String', count: 9 },
    ],
  },
  {
    name: 'gcp-video-logo-detection',
    predictions: [
      { label: 'Logo 1', count: 1 },
      { label: 'Logo 2', count: 3 },
      { label: 'Logo 3', count: 5 },
    ],
  },
  {
    name: 'gcp-video-object-detection',
    predictions: [
      { label: 'Object 1', count: 3 },
      { label: 'Object 2', count: 4 },
      { label: 'Object 3', count: 4 },
    ],
  },
  {
    name: 'gcp-video-text-detection',
    predictions: [
      { label: 'Text 1', count: 2 },
      { label: 'Text 2', count: 4 },
      { label: 'Text 3', count: 6 },
      { label: 'Text 4', count: 8 },
    ],
  },
]

const TimelineDetections = () => {
  const {
    query: { projectId },
  } = useRouter()

  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        overflow: 'auto',
        marginLeft: -constants.timeline.modulesWidth,
        borderTop: constants.borders.regular.smoke,
      }}
    >
      <div css={{ width: constants.timeline.modulesWidth }}>
        {TIMELINE_MODULES.map((module, index) => {
          const colorIndex = index % COLORS.length

          return (
            <TimelineAccordion
              key={module.name}
              moduleColor={COLORS[colorIndex]}
              cacheKey={`TimelineDetections.${projectId}.${module.name}`}
              module={module}
            >
              {module.predictions.map(({ label, count }) => {
                return (
                  <div key={label} css={{ display: 'flex' }}>
                    <div
                      css={{
                        width: COLOR_TAB_WIDTH,
                        backgroundColor: COLORS[colorIndex],
                      }}
                    />
                    <div
                      css={{
                        display: 'flex',
                        width: '100%',
                        borderTop: constants.borders.regular.smoke,
                        backgroundColor: colors.structure.coal,
                        overflow: 'hidden',
                      }}
                    >
                      <div
                        css={{
                          flex: 1,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          padding: spacing.base,
                          paddingLeft: spacing.base + spacing.spacious,
                          paddingRight: 0,
                        }}
                      >
                        {label}
                      </div>
                      <div css={{ padding: spacing.base }}>{`(${count})`}</div>
                    </div>
                  </div>
                )
              })}
            </TimelineAccordion>
          )
        })}
      </div>
      <div>{/* Insert marker zone here */}</div>
    </div>
  )
}

export default TimelineDetections

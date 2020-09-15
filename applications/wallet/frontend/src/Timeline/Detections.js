import { useRouter } from 'next/router'

import { colors, constants, spacing } from '../Styles'

import { useLocalStorageReducer } from '../LocalStorage/helpers'

import TimelineAccordion, { COLOR_TAB_WIDTH } from './Accordion'
import TimelineTracks from './Tracks'

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

const reducer = (state, action) => ({ ...state, ...action })

const TimelineDetections = () => {
  const {
    query: { assetId },
  } = useRouter()

  const [state, dispatch] = useLocalStorageReducer({
    key: `TimelineDetections.${assetId}`,
    reducer,
    initialState: {},
  })

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
        {TIMELINE_MODULES.map(({ name, predictions }, index) => {
          const colorIndex = index % COLORS.length

          return (
            <TimelineAccordion
              key={name}
              moduleColor={COLORS[colorIndex]}
              name={name}
              predictions={predictions}
              dispatch={dispatch}
              isOpen={state[name] || false}
            >
              {predictions.map(({ label, count }) => {
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
      <div css={{ flex: 1 }}>
        {TIMELINE_MODULES.map(({ name, predictions }) => {
          return (
            <TimelineTracks
              key={name}
              name={name}
              predictions={predictions}
              isOpen={state[name] || false}
            />
          )
        })}
      </div>
    </div>
  )
}

export default TimelineDetections

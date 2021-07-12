import PropTypes from 'prop-types'
import { useReducer } from 'react'

import SuspenseBoundary from '../SuspenseBoundary'

import { colors, constants, spacing, typography } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import Button, { VARIANTS } from '../Button'
import Resizeable from '../Resizeable'
import { ACTIONS, reducer as resizeableReducer } from '../Resizeable/reducer'

import PreviewSvg from '../Icons/preview.svg'

import { INITIAL_STATE, reducer } from './reducer'

import ModelMatrixControls from './Controls'
import ModelMatrixMatrix from './Matrix'
import ModelMatrixPreview from './Preview'
import { PANEL_WIDTH } from './helpers'

const ACCURACY_WIDTH = 40

const ModelMatrixLayout = ({
  projectId,
  modelId,
  matrixDetails: { name, overallAccuracy, labels, moduleName, datasetId },
  setMatrixDetails,
}) => {
  const [settings, dispatch] = useReducer(reducer, INITIAL_STATE)

  const [{ isOpen }, setPreviewSettings] = useLocalStorage({
    key: `Resizeable.ModelMatrixPreview`,
    reducer: resizeableReducer,
    initialState: {
      size: PANEL_WIDTH,
      originSize: 0,
      isOpen: false,
    },
  })

  return (
    <div
      css={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      {name && (
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            padding: spacing.normal,
            borderBottom: constants.borders.regular.coal,
            fontSize: typography.size.medium,
            lineHeight: typography.height.medium,
            backgroundColor: colors.structure.lead,
          }}
        >
          <span
            css={{
              fontWeight: typography.weight.bold,
              paddingRight: spacing.small,
              whiteSpace: 'nowrap',
            }}
          >
            Accuracy:
          </span>

          <div css={{ width: ACCURACY_WIDTH }}>{`${Math.round(
            overallAccuracy * 100,
          )}%`}</div>

          <ModelMatrixControls
            isNormalized={settings.isNormalized}
            dispatch={dispatch}
          />
        </div>
      )}

      <div
        css={{
          flex: 1,
          width: '100%',
          height: '100%',
          display: 'flex',
          backgroundColor: colors.structure.lead,
          overflow: 'hidden',
        }}
      >
        <div
          css={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            width: '0%',
          }}
        >
          <SuspenseBoundary>
            <ModelMatrixMatrix
              projectId={projectId}
              modelId={modelId}
              settings={settings}
              dispatch={dispatch}
              setMatrixDetails={setMatrixDetails}
            />
          </SuspenseBoundary>
        </div>

        <Resizeable
          storageName="Resizeable.ModelMatrixPreview"
          minSize={PANEL_WIDTH}
          openToThe="left"
          isInitiallyOpen={false}
          isDisabled={!isOpen}
        >
          <ModelMatrixPreview
            settings={settings}
            labels={labels}
            moduleName={moduleName}
            datasetId={datasetId}
          />
        </Resizeable>

        <div
          css={{
            display: 'flex',
            flexShrink: 0,
            alignItems: 'flex-start',
            borderLeft: constants.borders.regular.coal,
          }}
        >
          <Button
            aria-label="Preview"
            title="Preview"
            variant={VARIANTS.ICON}
            onClick={() =>
              setPreviewSettings({
                type: ACTIONS.TOGGLE_OPEN,
                payload: {
                  minSize: PANEL_WIDTH,
                },
              })
            }
            style={{
              flex: 'none',
              paddingTop: spacing.normal,
              paddingBottom: spacing.normal,
              borderBottom: constants.borders.regular.coal,
              color: isOpen ? colors.key.two : colors.structure.steel,
              ':hover': {
                backgroundColor: colors.structure.mattGrey,
              },
              borderRadius: constants.borderRadius.none,
            }}
          >
            <PreviewSvg width={constants.icons.regular} />
          </Button>
        </div>
      </div>
    </div>
  )
}

ModelMatrixLayout.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  matrixDetails: PropTypes.shape({
    name: PropTypes.string.isRequired,
    overallAccuracy: PropTypes.number.isRequired,
    labels: PropTypes.arrayOf(PropTypes.string),
    moduleName: PropTypes.string.isRequired,
    datasetId: PropTypes.string.isRequired,
  }).isRequired,
  setMatrixDetails: PropTypes.func.isRequired,
}

export default ModelMatrixLayout

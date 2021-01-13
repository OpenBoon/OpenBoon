import PropTypes from 'prop-types'
import { useReducer } from 'react'

import SuspenseBoundary from '../SuspenseBoundary'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import PreviewSvg from '../Icons/preview.svg'

import { INITIAL_STATE, reducer } from './reducer'

import ModelMatrixControls from './Controls'
import ModelMatrixMatrix from './Matrix'
import ModelMatrixPreview from './Preview'

const PANEL_WIDTH = 200
const ACCURACY_WIDTH = 40

const ModelMatrixLayout = ({
  projectId,
  modelId,
  matrixDetails: { name, overallAccuracy, labels },
  setMatrixDetails,
}) => {
  const [settings, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <div
      css={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
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
            Overall Accuracy:
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
          display: 'flex',
          backgroundColor: colors.structure.lead,
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

        {settings.isPreviewOpen && (
          <div
            css={{
              display: 'flex',
              flexDirection: 'column',
              width: PANEL_WIDTH,
              height: '100%',
              borderLeft: constants.borders.regular.coal,
              overflow: 'auto',
            }}
          >
            <ModelMatrixPreview
              selectedCell={settings.selectedCell}
              labels={labels}
            />
          </div>
        )}

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
              dispatch({
                isPreviewOpen: !settings.isPreviewOpen,
              })
            }
            style={{
              flex: 'none',
              paddingTop: spacing.normal,
              paddingBottom: spacing.normal,
              borderBottom: constants.borders.regular.coal,
              color: settings.isPreviewOpen
                ? colors.key.one
                : colors.structure.steel,
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
    labels: PropTypes.arrayOf(PropTypes.number),
  }).isRequired,
  setMatrixDetails: PropTypes.func.isRequired,
}

export default ModelMatrixLayout

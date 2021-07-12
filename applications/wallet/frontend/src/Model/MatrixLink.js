import PropTypes from 'prop-types'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, spacing, typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ModelMatrixMinimap from '../ModelMatrix/Minimap'
import ModelMatrixEmptyMinimap from '../ModelMatrix/EmptyMinimap'

const MINIMAP_WIDTH = 130
const TEXT_WIDTH = 200

const ModelMatrixLink = ({ projectId, model }) => {
  const { data: matrix } = useSWR(
    `/api/v1/projects/${projectId}/models/${model.id}/confusion_matrix/`,
  )

  if (!matrix.isMatrixApplicable) {
    return (
      <div css={{ display: 'flex' }}>
        <div css={{ width: MINIMAP_WIDTH, paddingRight: spacing.normal }}>
          <ModelMatrixEmptyMinimap
            isMatrixApplicable={matrix.isMatrixApplicable}
          />
        </div>

        <div
          css={{
            width: TEXT_WIDTH,
            color: colors.structure.steel,
            fontStyle: typography.style.italic,
          }}
        >
          A confusion matrix is not available for this model type.
        </div>
      </div>
    )
  }

  if (matrix.isMatrixApplicable && matrix.matrix.length === 0) {
    return (
      <div css={{ display: 'flex' }}>
        <div css={{ width: MINIMAP_WIDTH, paddingRight: spacing.normal }}>
          <ModelMatrixEmptyMinimap
            isMatrixApplicable={matrix.isMatrixApplicable}
          />
        </div>
        <div>
          <div
            css={{
              color: colors.structure.zinc,
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              fontFamily: typography.family.condensed,
              textTransform: 'uppercase',
              paddingBottom: spacing.base,
            }}
          >
            Confusion Matrix
          </div>
          <div
            css={{
              width: TEXT_WIDTH,
              fontStyle: typography.style.italic,
              color: colors.structure.steel,
            }}
          >
            {!model.datasetId &&
              'To view the matrix you must link the model to a dataset, add test labels, and run the "Test" or "Analyze All" actions.'}

            {!!model.datasetId &&
              !model.timeLastTrained &&
              'To view the matrix you must add test labels, and run the "Test" or "Analyze All" actions.'}

            {!!model.datasetId &&
              !!model.timeLastTrained &&
              !model.timeLastApplied &&
              'To view the matrix you must run the "Test" or "Analyze All" actions.'}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div css={{ display: 'flex' }}>
      <div css={{ width: MINIMAP_WIDTH, paddingRight: spacing.normal }}>
        <ModelMatrixMinimap
          matrix={matrix}
          settings={{
            width: 0,
            height: 0,
            labelsWidth: 0,
            isMinimapOpen: true,
            zoom: 1,
          }}
          isInteractive={false}
          isOutOfDate={model.unappliedChanges}
        />
      </div>

      <div css={{ width: TEXT_WIDTH }}>
        <div
          css={{
            color: colors.structure.zinc,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            fontFamily: typography.family.condensed,
            textTransform: 'uppercase',
            paddingBottom: spacing.normal,
          }}
        >
          <div css={{ paddingBottom: spacing.base }}>Confusion Matrix</div>

          {model.unappliedChanges ? (
            <div
              css={{
                width: TEXT_WIDTH,
                fontStyle: typography.style.italic,
                color: colors.structure.steel,
                textTransform: 'none',
              }}
            >
              The matrix is out of date <br /> and not representative.
            </div>
          ) : (
            <div>
              Accuracy:{' '}
              <span
                css={{
                  color: colors.structure.white,
                  fontFamily: typography.family.regular,
                }}
              >
                {Math.round(matrix.overallAccuracy * 100)}%
              </span>
            </div>
          )}
        </div>

        <Link href={`/${projectId}/models/${model.id}/matrix`} passHref>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            style={{
              width: 'min-content',
              padding: spacing.moderate,
            }}
          >
            View Matrix Details
          </Button>
        </Link>
      </div>
    </div>
  )
}

ModelMatrixLink.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    datasetId: PropTypes.string,
    unappliedChanges: PropTypes.bool.isRequired,
    timeLastTrained: PropTypes.number,
    timeLastApplied: PropTypes.number,
  }).isRequired,
}

export default ModelMatrixLink

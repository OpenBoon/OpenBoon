import PropTypes from 'prop-types'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, spacing, typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ModelMatrixMinimap from '../ModelMatrix/Minimap'
import ModelMatrixEmptyMinimap from '../ModelMatrix/EmptyMinimap'

const MINIMAP_WIDTH = 130
const TEXT_WIDTH = 200

const ModelMatrixLink = ({ projectId, modelId }) => {
  const { data: matrix } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/confusion_matrix/`,
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
        <div css={{ width: TEXT_WIDTH, fontStyle: typography.style.italic }}>
          To view the matrix, you must add test labels and train the model.
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
        />
      </div>
      <div css={{ width: TEXT_WIDTH }}>
        <div
          css={{
            fontWeight: typography.weight.bold,
            paddingBottom: spacing.normal,
          }}
        >
          Confusion Matrix <br />
          Overall Accuracy:{' '}
          <span css={{ fontWeight: typography.weight.regular }}>
            {Math.round(matrix.overallAccuracy * 100)}%
          </span>
        </div>

        <Link href={`/${projectId}/models/${modelId}/matrix`} passHref>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            style={{
              width: 'min-content',
              padding: spacing.moderate,
            }}
          >
            View Matrix
          </Button>
        </Link>
      </div>
    </div>
  )
}

ModelMatrixLink.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
}

export default ModelMatrixLink

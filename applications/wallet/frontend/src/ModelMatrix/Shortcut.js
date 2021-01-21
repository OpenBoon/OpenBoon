import PropTypes from 'prop-types'

import Link from 'next/link'

import { colors, spacing, typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ModelMatrixMinimap from './Minimap'

export const noop = () => () => {}

const MINIMAP_WIDTH = 130
const TEXT_WIDTH = 200

const ModelMatrixShortcut = ({ projectId, modelId, matrix }) => {
  return (
    <>
      {matrix.isMatrixApplicable && (
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
              isStatic
            />
          </div>
          <div css={{ width: TEXT_WIDTH }}>
            {matrix.matrix.length === 0 && (
              <div css={{ fontStyle: typography.style.italic }}>
                Train the model to view the confusion matrix.
              </div>
            )}

            {matrix.matrix.length > 0 && (
              <>
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

                <Link
                  href="/[projectId]/models/[modelId]/matrix"
                  as={`/${projectId}/models/${modelId}/matrix`}
                  passHref
                >
                  <Button
                    variant={BUTTON_VARIANTS.SECONDARY}
                    style={{
                      width: 'min-content',
                      padding: spacing.moderate,
                    }}
                    onClick={noop}
                  >
                    View Matrix
                  </Button>
                </Link>
              </>
            )}
          </div>
        </div>
      )}

      {!matrix.isMatrixApplicable && (
        <div
          css={{
            width: TEXT_WIDTH,
            color: colors.structure.steel,
            fontStyle: typography.style.italic,
          }}
        >
          A confusion matrix is not available for this model type.
        </div>
      )}
    </>
  )
}

ModelMatrixShortcut.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  matrix: PropTypes.shape({
    isMatrixApplicable: PropTypes.bool.isRequired,
    matrix: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)).isRequired,
    overallAccuracy: PropTypes.number.isRequired,
  }).isRequired,
}

export default ModelMatrixShortcut

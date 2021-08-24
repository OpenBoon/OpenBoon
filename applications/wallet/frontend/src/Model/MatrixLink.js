import PropTypes from 'prop-types'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ModelMatrixMinimap from '../ModelMatrix/Minimap'
import ModelMatrixEmptyMinimap from '../ModelMatrix/EmptyMinimap'

import CheckmarkSvg from '../Icons/checkmark.svg'

const MINIMAP_WIDTH = 130
const TEXT_WIDTH = 200

const ModelMatrixLink = ({ projectId, model }) => {
  const {
    id,
    modelTypeRestrictions: { missingLabels, missingLabelsOnAssets },
    datasetId,
    timeLastApplied,
    unappliedChanges,
    uploadable,
  } = model

  const { data: matrix } = useSWR(
    `/api/v1/projects/${projectId}/models/${id}/confusion_matrix/`,
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
        <div css={{ width: MINIMAP_WIDTH, marginRight: spacing.normal }}>
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

          <div css={{ width: TEXT_WIDTH }}>
            To view the matrix:
            <ul
              css={{
                margin: 0,
                padding: 0,
                paddingTop: spacing.base,
                li: {
                  lineHeight: '24px',
                  listStyleType: 'none',
                  display: 'flex',
                  alignItems: 'center',
                  'svg, span': {
                    paddingRight: spacing.base,
                  },
                },
              }}
            >
              <li
                css={{
                  color: !datasetId
                    ? colors.structure.zinc
                    : colors.structure.white,
                }}
              >
                {!datasetId ? (
                  <span
                    css={{
                      padding: spacing.small,
                      paddingTop: 0,
                      paddingBottom: 0,
                    }}
                  >
                    —
                  </span>
                ) : (
                  <CheckmarkSvg
                    height={constants.icons.regular}
                    color={colors.signal.grass.base}
                  />
                )}
                add a dataset
              </li>

              <li
                css={{
                  color:
                    !datasetId || !!missingLabels || !!missingLabelsOnAssets
                      ? colors.structure.zinc
                      : colors.structure.white,
                }}
              >
                {!datasetId || !!missingLabels || !!missingLabelsOnAssets ? (
                  <span
                    css={{
                      padding: spacing.small,
                      paddingTop: 0,
                      paddingBottom: 0,
                    }}
                  >
                    —
                  </span>
                ) : (
                  <CheckmarkSvg
                    height={constants.icons.regular}
                    color={colors.signal.grass.base}
                  />
                )}
                add test labels to dataset
              </li>

              <li
                css={{
                  color:
                    !datasetId ||
                    !!missingLabels ||
                    !!missingLabelsOnAssets ||
                    !timeLastApplied
                      ? colors.structure.zinc
                      : colors.structure.white,
                }}
              >
                {!datasetId ||
                !!missingLabels ||
                !!missingLabelsOnAssets ||
                !timeLastApplied ? (
                  <span
                    css={{
                      padding: spacing.small,
                      paddingTop: 0,
                      paddingBottom: 0,
                    }}
                  >
                    —
                  </span>
                ) : (
                  <CheckmarkSvg
                    height={constants.icons.regular}
                    color={colors.signal.grass.base}
                  />
                )}
                {uploadable ? 'run "Test Model"' : 'run "Train & Test Model"'}
              </li>
            </ul>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div css={{ display: 'flex' }}>
      <div css={{ width: MINIMAP_WIDTH, marginRight: spacing.normal }}>
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
          isOutOfDate={unappliedChanges}
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

          {unappliedChanges ? (
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

        <Link href={`/${projectId}/models/${id}/matrix`} passHref>
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
    modelTypeRestrictions: PropTypes.shape({
      missingLabels: PropTypes.number.isRequired,
      missingLabelsOnAssets: PropTypes.number.isRequired,
    }).isRequired,
    unappliedChanges: PropTypes.bool.isRequired,
    uploadable: PropTypes.bool.isRequired,
    timeLastTrained: PropTypes.number,
    timeLastApplied: PropTypes.number,
  }).isRequired,
}

export default ModelMatrixLink

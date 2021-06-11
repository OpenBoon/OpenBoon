import { useState } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import CrossSvg from '../Icons/cross.svg'

import { encode } from '../Filters/helpers'

import FlashMessageErrors from '../FlashMessage/Errors'
import Pagination from '../Pagination'
import Button, { VARIANTS } from '../Button'

import { onDelete } from './helpers'

const SIZE = 32
const DIMENSIONS = 150

const DatasetLabelsAssets = ({
  projectId,
  datasetId,
  page,
  datasetName,
  scope,
  label,
}) => {
  const [errors, setErrors] = useState({})

  const { query } = useRouter()

  const filter = encode({
    filters: [
      {
        type: 'label',
        attribute: `labels.${datasetName}`,
        dataSetId: datasetId,
        values: {
          scope,
          labels: [label],
        },
      },
    ],
  })

  const from = page * SIZE - SIZE

  const url = `/api/v1/projects/${projectId}/searches/query/?query=${filter}&from=${from}&size=${SIZE}`

  const { data } = useSWR(url)

  const { results = [], count } = data || {}

  return (
    <>
      <FlashMessageErrors
        errors={errors}
        styles={{ paddingTop: spacing.base, paddingBottom: spacing.base }}
      />

      <h3
        css={{
          color: colors.structure.zinc,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.regular,
          paddingTop: spacing.base,
          paddingBottom: spacing.normal,
        }}
      >
        Assets: {count}
      </h3>

      <div
        css={{
          display: 'flex',
          flexWrap: 'wrap',
          justifyAssets: 'flex-start',
        }}
      >
        {results.map(({ id, name, thumbnailUrl }) => {
          const { pathname: thumbnailSrc } = new URL(thumbnailUrl)

          return (
            <div
              key={id}
              css={{
                position: 'relative',
                border: constants.borders.large.transparent,
                width: DIMENSIONS,
                height: DIMENSIONS,
                ':hover': {
                  border: constants.borders.large.white,
                  button: {
                    opacity: 1,
                  },
                },
              }}
            >
              <div
                css={{
                  paddingBottom: '100%',
                  position: 'relative',
                  background: colors.structure.mattGrey,
                }}
              >
                <img
                  css={{
                    position: 'absolute',
                    top: 0,
                    width: '100%',
                    height: '100%',
                    objectFit: 'contain',
                  }}
                  src={thumbnailSrc}
                  alt={name}
                />
                <Button
                  variant={VARIANTS.NEUTRAL}
                  title="Remove from set"
                  aria-label="Remove from set"
                  onClick={() => {
                    onDelete({
                      query,
                      assetId: id,
                      label,
                      url,
                      setErrors,
                    })
                  }}
                  css={{
                    position: 'absolute',
                    top: spacing.mini,
                    left: spacing.mini,
                    color: 'white',
                    opacity: 0,
                    padding: spacing.base,
                    backgroundColor: `${colors.structure.smoke}${constants.opacity.hexHalf}`,
                    borderRadius: constants.borderRadius.small,
                    ':hover, &.focus-visible:focus': {
                      opacity: 1,
                      backgroundColor: colors.structure.smoke,
                    },
                  }}
                >
                  <CrossSvg height={constants.icons.regular} />
                </Button>
              </div>
            </div>
          )
        })}
      </div>

      {count > 0 && <div>&nbsp;</div>}

      {count > 0 && (
        <Pagination currentPage={page} totalPages={Math.ceil(count / SIZE)} />
      )}
    </>
  )
}

DatasetLabelsAssets.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  page: PropTypes.number.isRequired,
  datasetName: PropTypes.string.isRequired,
  scope: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
}

export default DatasetLabelsAssets

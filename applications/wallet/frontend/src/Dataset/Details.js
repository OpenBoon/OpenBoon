import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR, { mutate } from 'swr'
import Router from 'next/router'

import { spacing, constants } from '../Styles'

import ItemTitle from '../Item/Title'
import ItemList from '../Item/List'
import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import Modal from '../Modal'

import { fetcher } from '../Fetch/helpers'
import { decamelize } from '../Text/helpers'

import KebabSvg from '../Icons/kebab.svg'

const DatasetDetails = ({ projectId, datasetId }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  const {
    data: { name, type, description },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/${datasetId}/`)

  return (
    <div>
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
        }}
      >
        <ItemTitle type="Dataset" name={name} />

        <Menu
          open="bottom-left"
          button={({ onBlur, onClick }) => (
            <Button
              aria-label="Toggle Actions Menu"
              variant={VARIANTS.SECONDARY}
              onBlur={onBlur}
              onClick={onClick}
              style={{ padding: spacing.moderate, marginBottom: spacing.small }}
            >
              <KebabSvg height={constants.icons.regular} />
            </Button>
          )}
        >
          {({ onBlur, onClick }) => (
            <div>
              <ul>
                <li>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={async () => {
                      onClick()
                      setDeleteModalOpen(true)
                    }}
                  >
                    Delete Dataset
                  </Button>
                </li>
              </ul>
            </div>
          )}
        </Menu>

        {isDeleteModalOpen && (
          <Modal
            title="Delete Dataset"
            message="Are you sure you want to delete this dataset? Deleting will remove it from all linked models. Any labels that have been added by the model will remain."
            action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
            onCancel={() => {
              setDeleteModalOpen(false)
            }}
            onConfirm={async () => {
              setIsDeleting(true)

              await fetcher(
                `/api/v1/projects/${projectId}/datasets/${datasetId}/`,
                { method: 'DELETE' },
              )

              await mutate(`/api/v1/projects/${projectId}/datasets/`)

              Router.push(
                '/[projectId]/datasets/?action=delete-dataset-success',
                `/${projectId}/datasets/`,
              )
            }}
          />
        )}
      </div>

      <ItemList
        attributes={[
          ['Dataset Type', decamelize({ word: type })],
          ['Description', description],
        ]}
      />
    </div>
  )
}

DatasetDetails.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
}

export default DatasetDetails

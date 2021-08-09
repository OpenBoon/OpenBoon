import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const DatasetConceptsMenu = ({ projectId, datasetId, label, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={() => {
                    Router.push(
                      `/[projectId]/datasets/[datasetId]?edit=${label}`,
                      `/${projectId}/datasets/${datasetId}`,
                    )
                  }}
                >
                  Edit
                </Button>
              </li>

              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()
                    setDeleteModalOpen(true)
                  }}
                >
                  Delete
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>

      {isDeleteModalOpen && (
        <Modal
          title="Delete Concept"
          message={`Are you sure you want to delete the "${label}" concept? This cannot be undone.`}
          action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
          onCancel={() => {
            setDeleteModalOpen(false)
          }}
          onConfirm={async () => {
            setIsDeleting(true)

            await fetcher(
              `/api/v1/projects/${projectId}/datasets/${datasetId}/destroy_label/`,
              {
                method: 'DELETE',
                body: JSON.stringify({ label }),
              },
            )

            await revalidate()

            Router.push(
              '/[projectId]/datasets/[datasetId]?action=delete-concept-success',
              `/${projectId}/datasets/${datasetId}`,
            )
          }}
        />
      )}
    </>
  )
}

DatasetConceptsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DatasetConceptsMenu

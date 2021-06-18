import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const DatasetsMenu = ({ projectId, datasetId, revalidate }) => {
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

            await revalidate()

            Router.push(
              '/[projectId]/datasets?action=delete-dataset-success',
              `/${projectId}/datasets`,
            )
          }}
        />
      )}
    </>
  )
}

DatasetsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DatasetsMenu

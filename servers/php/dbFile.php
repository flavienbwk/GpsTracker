<?php

class dbFile {

	private $_file_path = null;
	private $_error = false;
	private $_message = "";
	private $_content = [];

	public function __construct($file_path) {
		$this->_file_path = $file_path;
	}

	public function setError($message) {
		$this->_error = true;
		$this->_message = $message;
	}

	public function setMessage($message) {
		$this->_message = $message;
	}

	public function getMessage() {
		return $this->_message;
	}

	public function hasError() {
		return $this->_error;
	}

	protected function isWritable($file_path) {
		if (!is_writable($this->_file_path) && false) {
			$this->setError("Impossible to write this file : " . $file_path);
			return false;
		}
		return true;
	}

	protected function loadContent($file_path) {
		if ($this->isWritable($file_path)) {
			$content = file_get_contents($file_path);
			if (!empty($content)) {
				if ($this->is_valid_json($content)) {
					$this->_content = json_decode($content, true);
				} else {
					$this->setError("Invalid JSON content.");
				}
			} else {
				$this->_content = [];
			}
		}
		return (!$this->_error);
	}

	public function is_valid_json( $raw_json ){
		return ( json_decode( $raw_json , true ) == NULL ) ? false : true ;
	}

	public function add($object) {
		if ($this->loadContent($this->_file_path)) {
			array_push($this->_content, $object);
			$this->save();
		}
	}

	protected function save() {
		if (file_put_contents($this->_file_path, json_encode($this->_content))) {
			return true;
		} else {
			$this->setError("Impossible to save this file.");
			return false;
		}
	}

}
